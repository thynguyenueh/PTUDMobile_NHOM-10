package com.example.studenthousing.data.repository;

import android.content.Context;
import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.remote.MessageRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import java.util.List;

public class ChatRepository {
    private final Context ctx;
    private final MessageDao local;
    private final MessageRemote remote;

    public ChatRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new MessageDao(this.ctx);
        this.remote = new MessageRemote();
    }

    public void getMessages(String roomId, AppCallback<List<Message>> cb) {
        List<Message> cached = local.getByRoomId(roomId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getMessages(roomId, new AppCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    local.upsertAll(messages);
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
        } else if (!cached.isEmpty()) {
            cb.onSuccess(cached);
        } else {
            cb.onError(new Exception("Không có mạng và không có cache tin nhắn"));
        }
    }

    public void sendMessage(String roomId, Message msg, AppCallback<String> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.sendMessage(roomId, msg, new AppCallback<String>() {
                @Override
                public void onSuccess(String msgId) {
                    msg.setMessageId(msgId);
                    local.upsert(msg);
                    cb.onSuccess(msgId);
                }

                @Override
                public void onError(Exception e) {
                    cb.onError(e);
                }
            });
        } else {
            cb.onError(new Exception("Cần kết nối mạng để gửi tin nhắn"));
        }
    }

    public void getUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.getUserRooms(userId, cb);
        } else {
            cb.onError(new Exception("Cần mạng để xem danh sách cuộc trò chuyện"));
        }
    }
}
