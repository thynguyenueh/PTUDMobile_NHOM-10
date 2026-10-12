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
        db.collection(COLLECTION)
                .add(r)
                .addOnSuccessListener(ref -> {
                    r.setReviewId(ref.getId());
                    cb.onSuccess(ref.getId());
                })
                .addOnFailureListener(cb::onError);
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

    // Query theo reviewee (người được đánh giá)
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

    // Query theo request (1 request có tối đa 2 review — 2 chiều)
    public void getByRequest(String requestId, AppCallback<List<Review>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("requestId", requestId)
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

    // Điểm đánh giá trung bình
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
