package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "exchange_requests";

    // Tạo request mới
    public void create(ExchangeRequest r, AppCallback<String> cb) {
        db.collection(COLLECTION)
                .add(r)
                .addOnSuccessListener(ref -> {
                    r.setRequestId(ref.getId());
                    cb.onSuccess(ref.getId());
                })
                .addOnFailureListener(cb::onError);
    }

    // Update
    public void update(ExchangeRequest r, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(r.getRequestId())
                .set(r)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Update chỉ field status
    public void updateStatus(String requestId, String status, AppCallback<Void> cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", System.currentTimeMillis());

        db.collection(COLLECTION).document(requestId)
                .update(updates)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Delete
    public void delete(String requestId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(requestId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Đọc danh sách request do user gửi
    public void getBySender(String senderId, AppCallback<List<ExchangeRequest>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("senderId", senderId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // Đọc danh sách request user nhận được
    public void getByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("receiverId", receiverId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // Get 1 request
    public void getById(String requestId, AppCallback<ExchangeRequest> cb) {
        db.collection(COLLECTION).document(requestId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        if (r != null) r.setRequestId(doc.getId());
                        cb.onSuccess(r);
                    } else {
                        cb.onError(new Exception("Request không tồn tại"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    // REALTIME LISTENER — cho user theo dõi request gửi/nhận
    public ListenerRegistration listenBySender(String senderId, AppCallback<List<ExchangeRequest>> cb) {
        return db.collection(COLLECTION)
                .whereEqualTo("senderId", senderId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;

                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        return db.collection(COLLECTION)
                .whereEqualTo("receiverId", receiverId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;

                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenById(String requestId, AppCallback<ExchangeRequest> cb) {
        return db.collection(COLLECTION).document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (doc == null || !doc.exists()) return;
                    ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                    if (r != null) r.setRequestId(doc.getId());
                    cb.onSuccess(r);
                });
    }
}
