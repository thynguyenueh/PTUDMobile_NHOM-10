package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PostRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "accommodation_posts";

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

    public void create(AccommodationPost p, AppCallback<String> cb) {
        String id = p.getPostId();
        if (id == null || id.isEmpty() || id.startsWith("temp_")) {
            id = db.collection(COLLECTION).document().getId();
            p.setPostId(id);
        }
        final String postId = id;
        db.collection(COLLECTION).document(postId).set(p)
                .addOnSuccessListener(a -> cb.onSuccess(postId))
                .addOnFailureListener(cb::onError);
    }

    public void update(AccommodationPost p, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(p.getPostId())
                .set(p)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void delete(String postId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(postId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
