package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ROOM_COLLECTION = "chat_rooms";
    private static final String MSG_SUBCOLLECTION = "messages";

    public void sendMessage(String roomId, Message msg, AppCallback<String> cb) {
        String messageId = msg.getMessageId();
        if (messageId == null || messageId.isEmpty()) {
            cb.onError(new IllegalArgumentException("Message ID is required for idempotent send"));
            return;
        }
        msg.setRoomId(roomId);
        msg.setRead(false);
        db.collection(ROOM_COLLECTION).document(roomId)
                .collection(MSG_SUBCOLLECTION).document(messageId)
                .set(msg)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> roomUpdate = new HashMap<>();
                    roomUpdate.put("lastMessage", msg.getContent());
                    roomUpdate.put("lastMessageTime", msg.getSentAt());
                    roomUpdate.put("lastSenderId", msg.getSenderId());
                    db.collection(ROOM_COLLECTION).document(roomId)
                            .set(roomUpdate, SetOptions.merge())
                            .addOnSuccessListener(ignored -> cb.onSuccess(messageId))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    public ListenerRegistration listenMessages(String roomId, AppCallback<List<Message>> cb) {
        return db.collection(ROOM_COLLECTION).document(roomId)
                .collection(MSG_SUBCOLLECTION)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;
                    List<Message> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Message m = doc.toObject(Message.class);
                        m.setMessageId(doc.getId());
                        list.add(m);
                    }
                    cb.onSuccess(list);
                });
    }

    public void getUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        db.collection(ROOM_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ChatRoom> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        room.setRoomId(doc.getId());
                        list.add(room);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getLastMessageTime(), r1.getLastMessageTime()));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public ListenerRegistration listenUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        return db.collection(ROOM_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;
                    List<ChatRoom> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        room.setRoomId(doc.getId());
                        list.add(room);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getLastMessageTime(), r1.getLastMessageTime()));
                    cb.onSuccess(list);
                });
    }

    public void markAsRead(String roomId, String currentUserId, AppCallback<Void> cb) {
        if (roomId == null || currentUserId == null) {
            if (cb != null) cb.onSuccess(null);
            return;
        }
        db.collection(ROOM_COLLECTION).document(roomId)
                .collection(MSG_SUBCOLLECTION)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        if (cb != null) cb.onSuccess(null);
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    boolean[] hasUnreadFromOther = {false};
                    for (QueryDocumentSnapshot doc : snap) {
                        Boolean readVal = doc.getBoolean("read");
                        Boolean isReadVal = doc.getBoolean("isRead");
                        boolean isRead = Boolean.TRUE.equals(readVal) || Boolean.TRUE.equals(isReadVal);
                        String senderId = doc.getString("senderId");

                        if (!isRead && !currentUserId.equals(senderId)) {
                            batch.update(doc.getReference(), "read", true);
                            hasUnreadFromOther[0] = true;
                        }
                    }
                    if (!hasUnreadFromOther[0]) {
                        if (cb != null) cb.onSuccess(null);
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (cb != null) cb.onSuccess(null); })
                            .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
                })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void ensureRoom(String roomId, List<String> participantIds,
                           String postId, String postTitle, AppCallback<Void> cb) {
        if (participantIds == null || participantIds.size() < 2) {
            if (cb != null) cb.onError(new IllegalArgumentException("Chat room requires at least 2 participants"));
            return;
        }

        List<String> safeParticipants = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String participant : participantIds) {
            if (participant == null) continue;
            String value = participant.trim();
            if (value.isEmpty() || !seen.add(value)) continue;
            safeParticipants.add(value);
        }
        if (safeParticipants.size() < 2) {
            if (cb != null) cb.onError(new IllegalArgumentException("Chat room requires 2 distinct participants"));
            return;
        }

        String safeRoomId = roomId == null || roomId.trim().isEmpty()
                ? safeParticipants.get(0) + "_" + safeParticipants.get(1)
                : roomId.trim();

        Map<String, Object> data = new HashMap<>();
        data.put("participantIds", safeParticipants);
        if (postId != null && !postId.isEmpty()) data.put("postId", postId);
        if (postTitle != null && !postTitle.isEmpty()) data.put("postTitle", postTitle);
        db.collection(ROOM_COLLECTION).document(safeRoomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) data.put("createdAt", System.currentTimeMillis());
                    db.collection(ROOM_COLLECTION).document(safeRoomId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(a -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }
}
