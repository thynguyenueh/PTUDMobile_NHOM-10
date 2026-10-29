package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "accommodation_posts";

    /** Tất cả field đều ghi vào document chính — khớp hasPublicPostFields() của rules. */
    public static Map<String, Object> toPostData(AccommodationPost post) {
        Map<String, Object> data = new HashMap<>();

        data.put("userId", post.getUserId());
        data.put("familyName", post.getFamilyName());

        data.put("title", post.getTitle());
        data.put("description", post.getDescription());
        data.put("images", post.getImages() != null ? post.getImages() : new ArrayList<>());
        data.put("utilities", post.getUtilities() != null ? post.getUtilities() : new ArrayList<>());

        data.put("address", post.getAddress());
        data.put("streetName", post.getStreetName());
        data.put("houseNumber", post.getHouseNumber());
        data.put("cityCode", post.getCityCode());
        data.put("wardCode", post.getWardCode());
        data.put("cityName", post.getCityName());
        data.put("wardName", post.getWardName());
        data.put("addressValidationStatus", post.getAddressValidationStatus());

        data.put("latitude", post.getLatitude());
        data.put("longitude", post.getLongitude());

        data.put("nearbyCampuses", post.getNearbyCampuses());
        List<String> campusIds = post.getNearbyCampusCodes();
        data.put("nearbyCampusIds", campusIds != null ? campusIds : new ArrayList<>());
        data.put("distanceKm", post.getDistanceKm());
        data.put("distanceSource", post.getDistanceSource());
        data.put("distanceEstimate", post.isDistanceEstimate());

        data.put("status", post.getStatus());
        data.put("createdAt", post.getCreatedAt());
        data.put("updatedAt", post.getUpdatedAt());
        data.put("syncStatus", post.getSyncStatus());

        return data;
    }

    public void getLatest(int limit, AppCallback<List<AccommodationPost>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("status", Constants.POST_AVAILABLE)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snap -> {
                    List<AccommodationPost> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        AccommodationPost p = doc.toObject(AccommodationPost.class);
                        if (p != null) p.setPostId(doc.getId());
                        list.add(p);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getById(String postId, AppCallback<AccommodationPost> cb) {
        db.collection(COLLECTION).document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        cb.onError(new Exception("Post not found"));
                        return;
                    }
                    AccommodationPost p = doc.toObject(AccommodationPost.class);
                    if (p == null) {
                        cb.onError(new Exception("Post object null"));
                        return;
                    }
                    p.setPostId(doc.getId());
                    cb.onSuccess(p);
                })
                .addOnFailureListener(cb::onError);
    }

    public void create(AccommodationPost p, AppCallback<String> cb) {
        String id = p.getPostId();
        if (id == null || id.isEmpty() || id.startsWith("temp_")) {
            id = db.collection(COLLECTION).document().getId();
            p.setPostId(id);
        }
        final String postId = id;
        DocumentReference ref = db.collection(COLLECTION).document(postId);
        ref.set(toPostData(p))
                .addOnSuccessListener(a -> cb.onSuccess(postId))
                .addOnFailureListener(cb::onError);
    }

    public void update(AccommodationPost p, AppCallback<Void> cb) {
        if (p == null || p.getPostId() == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("Post or PostId is null"));
            return;
        }
        db.collection(COLLECTION).document(p.getPostId())
                .set(toPostData(p), SetOptions.merge())
                .addOnSuccessListener(a -> { if (cb != null) cb.onSuccess(null); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void delete(String postId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(postId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}