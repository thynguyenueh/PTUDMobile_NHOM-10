package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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
    private static final String PRIVATE_SUBCOLLECTION = "private";
    private static final String LOCATION_DOC = "location";

    public static Map<String, Object> toPublicPostData(AccommodationPost post) {
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

    private static String publicAddress(AccommodationPost post) {
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
                        p.setPostId(doc.getId());
                        list.add(p);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getById(String postId, AppCallback<AccommodationPost> cb) {
        db.collection(COLLECTION).document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        AccommodationPost p = doc.toObject(AccommodationPost.class);
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

    public void create(AccommodationPost p, AppCallback<String> cb) {
        String id = p.getPostId();
        if (id == null || id.isEmpty() || id.startsWith("temp_")) {
            id = db.collection(COLLECTION).document().getId();
            p.setPostId(id);
        }
        final String postId = id;

        // 1. Tách thông tin công khai cấp Phường/Quận
        savePostPublicAndPrivate(postId, p, cb, false);
    }

    public void update(AccommodationPost p, AppCallback<Void> cb) {
        if (p == null || p.getPostId() == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("Post or PostId is null"));
            return;
        }
        savePostPublicAndPrivate(p.getPostId(), p, new AppCallback<String>() {
            @Override public void onSuccess(String id) { if (cb != null) cb.onSuccess(null); }
            @Override public void onError(Exception e) { if (cb != null) cb.onError(e); }
        }, true);
    }

    private void savePostPublicAndPrivate(String postId, AccommodationPost p, AppCallback<String> cb, boolean merge) {
        // Tách số nhà/tên đường chi tiết sang subcollection location
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("ownerUserId", p.getUserId());
        locationData.put("fullAddress", p.getAddress());
        locationData.put("houseNumber", p.getHouseNumber());
        locationData.put("streetName", p.getStreetName());
        locationData.put("latitude", p.getLatitude());
        locationData.put("longitude", p.getLongitude());

        DocumentReference postRef = db.collection(COLLECTION).document(postId);
        Map<String, Object> publicPostData = toPublicPostData(p);
        Task<Void> saveTask = merge
                ? postRef.set(publicPostData, SetOptions.merge())
                : postRef.set(publicPostData);
        saveTask
                .addOnSuccessListener(a -> {
                    db.collection(COLLECTION).document(postId)
                            .collection(PRIVATE_SUBCOLLECTION).document(LOCATION_DOC)
                            .set(locationData)
                            .addOnSuccessListener(unused -> cb.onSuccess(postId))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    public void delete(String postId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(postId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
