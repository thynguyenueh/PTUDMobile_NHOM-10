package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.util.AppCallback;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "accommodation_posts";
    private static final String PRIVATE_SUBCOLLECTION = "private";
    private static final String LOCATION_DOC = "location";

    public static Map<String, Object> toPublicPostData(Post post) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", post.getUserId());
        data.put("familyName", post.getFamilyName());
        data.put("title", post.getTitle());
        data.put("address", publicAddress(post));
        data.put("streetName", post.getStreetName());
        data.put("addressFingerprint", post.getAddressFingerprint());
        data.put("addressVerificationStatus", post.getAddressVerificationStatus());
        data.put("cityCode", post.getCityCode());
        data.put("wardCode", post.getWardCode());
        data.put("cityName", post.getCityName());
        data.put("wardName", post.getWardName());
        data.put("nearbyCampuses", post.getNearbyCampuses());
        data.put("nearbyCampusIds", post.getNearbyCampusIds());
        data.put("distanceKm", post.getDistanceKm());
        data.put("distanceSource", post.getDistanceSource());
        data.put("distanceEstimate", post.isDistanceEstimate());
        data.put("description", post.getDescription());
        data.put("images", post.getImages());
        data.put("utilities", post.getUtilities());
        data.put("status", post.getStatus());
        data.put("createdAt", post.getCreatedAt());
        data.put("updatedAt", post.getUpdatedAt());
        data.put("syncStatus", post.getSyncStatus());
        return data;
    }

    private static String publicAddress(Post post) {
        String street = post.getStreetName() == null ? "" : post.getStreetName().trim();
        String ward = post.getWardName() == null ? "" : post.getWardName().trim();
        String city = post.getCityName() == null ? "" : post.getCityName().trim();
        StringBuilder sb = new StringBuilder();
        if (!street.isEmpty()) sb.append(street);
        if (!ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }
        if (!city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        return sb.toString();
    }

    public void getLatest(int limit, AppCallback<List<Post>> cb) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Post> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Post p = doc.toObject(Post.class);
                        p.setPostId(doc.getId());
                        String status = p.getStatus();
                        if (status == null || status.trim().isEmpty() || "AVAILABLE".equalsIgnoreCase(status)) {
                            list.add(p);
                        }
                    }
                    list.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                    if (limit > 0 && list.size() > limit) {
                        list = list.subList(0, limit);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getById(String postId, AppCallback<Post> cb) {
        db.collection(COLLECTION).document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Post p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            // Thử đọc subcollection location nếu có quyền (đang trong giao dịch EXCHANGING)
                            getPostLocation(postId, new AppCallback<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot locDoc) {
                                    if (locDoc != null && locDoc.exists()) {
                                        if (locDoc.contains("fullAddress")) p.setAddress(locDoc.getString("fullAddress"));
                                        if (locDoc.contains("houseNumber")) p.setHouseNumber(locDoc.getString("houseNumber"));
                                        if (locDoc.contains("streetName")) p.setStreetName(locDoc.getString("streetName"));
                                    }
                                    cb.onSuccess(p);
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (e instanceof FirebaseFirestoreException
                                            && ((FirebaseFirestoreException) e).getCode()
                                            == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        cb.onSuccess(p);
                                    } else {
                                        cb.onError(e);
                                    }
                                }
                            });
                        } else {
                            cb.onError(new Exception("Post object null"));
                        }
                    } else {
                        cb.onError(new Exception("Post not found"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void getPostLocation(String postId, AppCallback<DocumentSnapshot> cb) {
        db.collection(COLLECTION).document(postId)
                .collection(PRIVATE_SUBCOLLECTION).document(LOCATION_DOC)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void create(Post p, AppCallback<String> cb) {
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    executeCreatePost(p, cb);
                } else {
                    if (cb != null) cb.onError(task.getException() != null ? task.getException() : new Exception("Auth failed"));
                }
            });
        } else {
            executeCreatePost(p, cb);
        }
    }

    private void executeCreatePost(Post p, AppCallback<String> cb) {
        com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            p.setUserId(fbUser.getUid());
        }
        String id = p.getPostId();
        if (id == null || id.isEmpty() || id.startsWith("temp_")) {
            id = db.collection(COLLECTION).document().getId();
            p.setPostId(id);
        }
        final String postId = id;

        // 1. Tách thông tin công khai cấp Phường/Quận
        savePostPublicAndPrivate(postId, p, cb, false);
    }

    public void update(Post p, AppCallback<Void> cb) {
        if (p == null || p.getPostId() == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("Post or PostId is null"));
            return;
        }
        savePostPublicAndPrivate(p.getPostId(), p, new AppCallback<String>() {
            @Override public void onSuccess(String id) { if (cb != null) cb.onSuccess(null); }
            @Override public void onError(Exception e) { if (cb != null) cb.onError(e); }
        }, true);
    }

    private void savePostPublicAndPrivate(String postId, Post p, AppCallback<String> cb, boolean merge) {
        DocumentReference postRef = db.collection(COLLECTION).document(postId);
        Map<String, Object> publicPostData = toPublicPostData(p);
        Task<Void> saveTask = merge
                ? postRef.set(publicPostData, SetOptions.merge())
                : postRef.set(publicPostData);
        saveTask
                .addOnSuccessListener(a -> cb.onSuccess(postId))
                .addOnFailureListener(e -> {
                    android.util.Log.e("PostRemote", "Failed to save post " + postId + ": " + e.getMessage(), e);
                    cb.onError(e);
                });
    }

    public void delete(String postId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(postId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
