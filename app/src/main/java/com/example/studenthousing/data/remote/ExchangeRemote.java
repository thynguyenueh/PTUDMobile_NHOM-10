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

    public void create(ExchangeRequest r, AppCallback<String> cb) {
        String requestId = r.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            cb.onError(new IllegalArgumentException("Request ID is required for idempotent create"));
            return;
        }
        db.collection(COLLECTION).document(requestId).set(r)
                .addOnSuccessListener(unused -> cb.onSuccess(requestId))
                .addOnFailureListener(cb::onError);
    }

    public void update(ExchangeRequest r, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(r.getRequestId())
                .set(r)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void updateStatus(String requestId, String status, AppCallback<Void> cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", System.currentTimeMillis());

        db.collection(COLLECTION).document(requestId)
                .update(updates)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void delete(String requestId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(requestId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

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
