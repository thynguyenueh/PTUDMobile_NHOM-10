package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private static final String POSTS_COLLECTION = "accommodation_posts";

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
        long now = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", now);
        updates.put("updatedAt", now);
        if (Constants.REQ_COMPLETED.equalsIgnoreCase(status)) {
            updates.put("completedAt", now);
        }

        if (Constants.REQ_EXCHANGING.equalsIgnoreCase(status)) {
            DocumentReference requestRef = db.collection(COLLECTION).document(requestId);
            final String[] postIds = new String[2];

            db.runTransaction(transaction -> {
                DocumentSnapshot request = transaction.get(requestRef);
                if (!request.exists()) {
                    throw new IllegalStateException("Exchange request not found");
                }

                String senderPostId = request.getString("senderPostId");
                String receiverPostId = request.getString("receiverPostId");
                postIds[0] = senderPostId;
                postIds[1] = receiverPostId;

                if (senderPostId == null || receiverPostId == null || senderPostId.equals(receiverPostId)) {
                    throw new IllegalStateException("Exchange request has invalid post references");
                }

                DocumentReference senderPostRef = db.collection(POSTS_COLLECTION).document(senderPostId);
                DocumentReference receiverPostRef = db.collection(POSTS_COLLECTION).document(receiverPostId);
                DocumentSnapshot senderPost = transaction.get(senderPostRef);
                DocumentSnapshot receiverPost = transaction.get(receiverPostRef);
                if (!senderPost.exists() || !receiverPost.exists()) {
                    throw new IllegalStateException("Exchange request references a missing post");
                }

                // 1. Cập nhật request được chấp nhận -> EXCHANGING
                transaction.update(requestRef, updates);

                // 2. Chuyển 2 bài đăng sang EXCHANGED
                Map<String, Object> postUpdates = new HashMap<>();
                postUpdates.put("status", Constants.POST_EXCHANGED);
                postUpdates.put("updatedAt", now);
                postUpdates.put("activeExchangeRequestId", requestId);
                transaction.update(senderPostRef, postUpdates);
                transaction.update(receiverPostRef, postUpdates);

                return null;
            }).addOnSuccessListener(unused -> {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;
                rejectOtherPendingRequests(postIds[1], requestId, currentUserId);
                cb.onSuccess(null);
            }).addOnFailureListener(cb::onError);
            return;
        }

        db.collection(COLLECTION).document(requestId)
                .update(updates)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private void rejectOtherPendingRequests(String receiverPostId, String currentRequestId, String currentUserId) {
        if (receiverPostId == null || currentUserId == null) return;
        long now = System.currentTimeMillis();
        Map<String, Object> rejectUpdates = new HashMap<>();
        rejectUpdates.put("status", Constants.REQ_REJECTED);
        rejectUpdates.put("rejectReason", Constants.REJECT_REASON_TAKEN);
        rejectUpdates.put("respondedAt", now);
        rejectUpdates.put("updatedAt", now);

        db.collection(COLLECTION)
                .whereEqualTo("status", Constants.REQ_PENDING)
                .whereEqualTo("receiverPostId", receiverPostId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        if (doc.getId().equals(currentRequestId)) continue;
                        String receiverId = doc.getString("receiverId");
                        if (!currentUserId.equals(receiverId)) continue;
                        doc.getReference().update(rejectUpdates);
                    }
                });
    }

    /**
     * Hủy thỏa thuận 2 bên:
     * - Bên A bấm hủy -> lưu cancelRequestedBy = A_uid, cancelRequestedAt = now.
     * - Bên B (khác A) bấm đồng ý -> cập nhật status = CANCELLED.
     */
    public void requestOrConfirmCancel(ExchangeRequest request, String currentUserId, AppCallback<Boolean> cb) {
        String requestId = request.getRequestId();
        String cancelRequestedBy = request.getCancelRequestedBy();

        if (cancelRequestedBy == null || cancelRequestedBy.trim().isEmpty()) {
            long now = System.currentTimeMillis();
            Map<String, Object> updates = new HashMap<>();
            updates.put("cancelRequestedBy", currentUserId);
            updates.put("cancelRequestedAt", now);
            updates.put("updatedAt", now);

            db.collection(COLLECTION).document(requestId)
                    .update(updates)
                    .addOnSuccessListener(a -> cb.onSuccess(false))
                    .addOnFailureListener(cb::onError);
        } else if (cancelRequestedBy.equalsIgnoreCase(currentUserId)) {
            cb.onError(new Exception("Bạn đã gửi yêu cầu hủy trước đó. Đang chờ đối tác xác nhận đồng ý."));
        } else {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Constants.REQ_CANCELLED);
            updates.put("respondedAt", System.currentTimeMillis());
            updates.put("updatedAt", System.currentTimeMillis());

            db.collection(COLLECTION).document(requestId)
                    .update(updates)
                    .addOnSuccessListener(a -> cb.onSuccess(true))
                    .addOnFailureListener(cb::onError);
        }
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
                .get()
                .addOnSuccessListener(snap -> {
                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
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
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;

                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        return db.collection(COLLECTION)
                .whereEqualTo("receiverId", receiverId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e); return; }
                    if (snap == null) return;

                    List<ExchangeRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        ExchangeRequest r = doc.toObject(ExchangeRequest.class);
                        r.setRequestId(doc.getId());
                        list.add(r);
                    }
                    list.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
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
