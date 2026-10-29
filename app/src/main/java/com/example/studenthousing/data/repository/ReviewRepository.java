package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.remote.ReviewRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

public class ReviewRepository {
    private final Context ctx;
    private final ReviewDao local;
    private final ReviewRemote remote;

    public ReviewRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new ReviewDao(this.ctx);
        this.remote = new ReviewRemote();
    }

    public void create(Review r, AppCallback<String> cb) {
        if (r == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("Review is null"));
            return;
        }

        String reviewerId = r.getReviewerId() != null ? r.getReviewerId().trim() : null;
        String revieweeId = r.getRevieweeId() != null ? r.getRevieweeId().trim() : null;

        if (reviewerId == null || reviewerId.isEmpty()
                || revieweeId == null || revieweeId.isEmpty()) {
            if (cb != null) cb.onError(new IllegalArgumentException("Review reviewerId and revieweeId are required"));
            return;
        }

        double rating = r.getRating();
        if (rating < 1.0 || rating > 5.0) {
            if (cb != null) cb.onError(new IllegalArgumentException("Rating must be between 1 and 5"));
            return;
        }

        if (reviewerId.equals(revieweeId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Reviewer and reviewee must be different users"));
            return;
        }

        r.setReviewerId(reviewerId);
        r.setRevieweeId(revieweeId);
        r.setReviewId(revieweeId + "_" + reviewerId);
        long now = System.currentTimeMillis();
        if (r.getCreatedAt() == 0) r.setCreatedAt(now);
        r.setUpdatedAt(now);

        if (!NetworkUtils.isOnline(ctx)) {
            r.setSyncStatus("PENDING_CREATE");
            local.upsert(r);
            if (cb != null) cb.onSuccess(r.getReviewId());
            return;
        }

        remote.create(r, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                r.setReviewId(id);
                r.setSyncStatus("SYNCED");
                local.upsert(r);
                if (cb != null) cb.onSuccess(id);
            }

            @Override
            public void onError(Exception e) {
                if (e instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
                    if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                            || code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                            || code == FirebaseFirestoreException.Code.ALREADY_EXISTS
                            || code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                            || code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                        if (cb != null) cb.onError(e);
                        return;
                    }
                }
                r.setSyncStatus("PENDING_CREATE");
                local.upsert(r);
                if (cb != null) cb.onSuccess(r.getReviewId());
            }
        });
    }

    public void getByReviewee(String revieweeId, AppCallback<List<Review>> cb) {
        List<Review> cached = local.getByReviewee(revieweeId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getByReviewee(revieweeId, new AppCallback<List<Review>>() {
                @Override
                public void onSuccess(List<Review> list) {
                    local.upsertAll(list);
                    cb.onSuccess(list);
                }

                @Override
                public void onError(Exception e) {
                    if (!cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onError(e);
                    }
                }
            });
        } else if (!cached.isEmpty()) {
            cb.onSuccess(cached);
        } else {
            cb.onError(new Exception("Không có mạng và không có cache"));
        }
    }

    public void getAverageRating(String revieweeId, AppCallback<Double> cb) {
        getByReviewee(revieweeId, new AppCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> list) {
                if (list == null || list.isEmpty()) {
                    if (cb != null) cb.onSuccess(5.0);
                    return;
                }
                double total = 0;
                for (Review r : list) total += r.getRating();
                if (cb != null) cb.onSuccess(total / list.size());
            }

            @Override
            public void onError(Exception e) {
                if (cb != null) cb.onSuccess(5.0);
            }
        });
    }
}
