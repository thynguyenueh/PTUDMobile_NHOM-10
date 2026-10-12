package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MessageRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ROOM_COLLECTION = "chat_rooms";
    private static final String MSG_SUBCOLLECTION = "messages";

    public void sendMessage(String roomId, Message msg, AppCallback<String> cb) {
        db.collection(ROOM_COLLECTION).document(roomId)
                .collection(MSG_SUBCOLLECTION)
                .add(msg)
                .addOnSuccessListener(ref -> {
                    msg.setMessageId(ref.getId());
                    // Update room's last message
                    db.collection(ROOM_COLLECTION).document(roomId)
                            .update("lastMessage", msg.getContent(),
                                    "lastMessageTime", msg.getSentAt(),
                                    "lastSenderId", msg.getSenderId());
                    cb.onSuccess(ref.getId());
                })
                .addOnFailureListener(cb::onError);
    }

    public void getMessages(String roomId, AppCallback<List<Message>> cb) {
        db.collection(ROOM_COLLECTION).document(roomId)
                .collection(MSG_SUBCOLLECTION)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Message> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Message m = doc.toObject(Message.class);
                        m.setMessageId(doc.getId());
                        list.add(m);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getUserRooms(String userId, AppCallback<List<ChatRoom>> cb) {
        db.collection(ROOM_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ChatRoom> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        room.setRoomId(doc.getId());
                        list.add(room);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }
}
