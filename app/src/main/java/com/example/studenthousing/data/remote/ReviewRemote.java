package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "reviews";

    public void create(Review r, AppCallback<String> cb) {
        if (r == null || r.getReviewerId() == null || r.getReviewerId().trim().isEmpty()
                || r.getRevieweeId() == null || r.getRevieweeId().trim().isEmpty()) {
            if (cb != null) cb.onError(new IllegalArgumentException("Review reviewerId and revieweeId are required"));
            return;
        }

        String reviewerId = r.getReviewerId().trim();
        String revieweeId = r.getRevieweeId().trim();
        String id = revieweeId + "_" + reviewerId;
        r.setReviewerId(reviewerId);
        r.setRevieweeId(revieweeId);
        r.setReviewId(id);
        final String reviewId = id;
        db.collection(COLLECTION).document(reviewId)
                .set(r)
                .addOnSuccessListener(ref -> {
                    if (cb != null) cb.onSuccess(reviewId);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void update(Review r, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(r.getReviewId())
                .set(r)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void delete(String reviewId, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(reviewId)
                .delete()
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getByReviewee(String revieweeId, AppCallback<List<Review>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("revieweeId", revieweeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Review r = doc.toObject(Review.class);
                        r.setReviewId(doc.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAverageRating(String revieweeId, AppCallback<Double> cb) {
        getByReviewee(revieweeId, new AppCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                if (reviews.isEmpty()) {
                    cb.onSuccess(0.0);
                    return;
                }
                double sum = 0;
                for (Review r : reviews) sum += r.getRating();
                cb.onSuccess(sum / reviews.size());
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }
}
