package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PostRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "accommodation_posts";

    // Lấy 50 bài mới nhất
    public void getLatest(int limit, AppCallback<List<AccommodationPost>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("status", "AVAILABLE")
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

    // Tạo bài mới
    public void create(AccommodationPost p, AppCallback<String> cb) {
        db.collection(COLLECTION)
                .add(p)
                .addOnSuccessListener(ref -> {
                    p.setPostId(ref.getId());
                    cb.onSuccess(ref.getId());
                })
                .addOnFailureListener(cb::onError);
    }

    // Update
    public void update(AccommodationPost p, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(p.getPostId())
                .set(p)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Delete
    public void delete(String postId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(postId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
