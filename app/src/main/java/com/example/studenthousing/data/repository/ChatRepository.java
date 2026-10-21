package com.example.studenthousing.data.repository;

import android.content.Context;
import com.example.studenthousing.data.local.ChatRoomDao;
import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.remote.MessageRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class ChatRepository {
    private final Context ctx;
    private final MessageDao localMessages;
    private final ChatRoomDao localRooms;
    private final MessageRemote remote;

    public ChatRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.localMessages = new MessageDao(this.ctx);
        this.localRooms = new ChatRoomDao(this.ctx);
        this.remote = new MessageRemote();
    }

    public String getRoomId(String uidA, String uidB) {
        if (uidA == null || uidB == null) return "room_default";
        if (uidA.compareTo(uidB) < 0) return uidA + "_" + uidB;
        else return uidB + "_" + uidA;
    }

    public void ensureRoom(String roomId, List<String> participants,
                           String postId, String postTitle, AppCallback<Void> cb) {
        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setParticipantIds(participants);
        room.setPostId(postId);
        room.setPostTitle(postTitle);
        room.setCreatedAt(System.currentTimeMillis());
        localRooms.upsert(room);

        if (!NetworkUtils.isOnline(ctx)) {
            if (cb != null) cb.onSuccess(null);
            return;
        }
        remote.ensureRoom(roomId, participants, postId, postTitle, new AppCallback<Void>() {
            @Override public void onSuccess(Void value) {
                if (cb != null) cb.onSuccess(null);
            }
            @Override public void onError(Exception e) {
                if (cb != null) cb.onSuccess(null);
            }
        });
    }

    public ListenerRegistration listenMessages(String roomId, AppCallback<List<Message>> cb) {
        List<Message> cached = localMessages.getByRoomId(roomId);
        if (!cached.isEmpty()) {
            cb.onSuccess(cached);
        }

        if (NetworkUtils.isOnline(ctx)) {
            return remote.listenMessages(roomId, new AppCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    localMessages.upsertAll(messages);
                    cb.onSuccess(messages);
                }

                @Override
                public void onError(Exception e) {
                    if (!cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onError(e);
                    }
                }
            });
        }
        return null;
    }

    public void sendMessage(String roomId, Message msg, AppCallback<String> cb) {
        if (msg.getMessageId() == null || msg.getMessageId().isEmpty()) {
            msg.setMessageId(java.util.UUID.randomUUID().toString());
        }
        msg.setRoomId(roomId);

        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setLastMessage("IMAGE".equals(msg.getType()) ? "[Hình ảnh]" : msg.getContent());
        room.setLastMessageTime(msg.getSentAt());
        room.setLastSenderId(msg.getSenderId());
        localRooms.upsert(room);

        if (!NetworkUtils.isOnline(ctx)) {
            msg.setSyncStatus("PENDING_CREATE");
            localMessages.upsert(msg);
            if (cb != null) cb.onSuccess(msg.getMessageId());
            return;
        }

        remote.sendMessage(roomId, msg, new AppCallback<String>() {
            @Override
            public void onSuccess(String msgId) {
                msg.setMessageId(msgId);
                msg.setSyncStatus("SYNCED");
                localMessages.upsert(msg);
                if (cb != null) cb.onSuccess(msgId);
            }
            @Override
            public void onError(Exception e) {
                msg.setSyncStatus("PENDING_CREATE");
                localMessages.upsert(msg);
                if (cb != null) cb.onSuccess(msg.getMessageId());
            }
        });
    }

    public void markAsRead(String roomId, String currentUserId, AppCallback<Void> cb) {
        localMessages.markAsRead(roomId, currentUserId);
        if (NetworkUtils.isOnline(ctx)) {
            remote.markAsRead(roomId, currentUserId, cb);
        } else if (cb != null) {
            cb.onSuccess(null);
        }
    }

    public List<ChatRoom> getUserRoomsCached(String userId) {
        return localRooms.getByUserId(userId);
    }

    public void getUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        List<ChatRoom> cached = localRooms.getByUserId(userId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getUserRooms(userId, new AppCallback<List<ChatRoom>>() {
                @Override
                public void onSuccess(List<ChatRoom> rooms) {
                    if (rooms != null) {
                        localRooms.upsertAll(rooms);
                        cb.onSuccess(rooms);
                    } else if (!cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onSuccess(new java.util.ArrayList<>());
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (!cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onError(e);
                    }
                }
            });
        } else if (!cached.isEmpty()) {
            cb.onSuccess(cached);
        } else {
            cb.onSuccess(new java.util.ArrayList<>());
        }
    }

    public ListenerRegistration listenUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        List<ChatRoom> cached = localRooms.getByUserId(userId);
        if (!cached.isEmpty()) {
            cb.onSuccess(cached);
        }

        if (NetworkUtils.isOnline(ctx)) {
            return remote.listenUserRooms(userId, new AppCallback<List<ChatRoom>>() {
                @Override
                public void onSuccess(List<ChatRoom> rooms) {
                    if (rooms != null) {
                        localRooms.upsertAll(rooms);
                        cb.onSuccess(rooms);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (!cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onError(e);
                    }
                }
            });
        }
        return null;
    }
}
