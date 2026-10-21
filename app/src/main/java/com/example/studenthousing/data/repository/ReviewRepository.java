package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.remote.ReviewRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;

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
        if (r.getCreatedAt() == 0) r.setCreatedAt(System.currentTimeMillis());
        if (r.getReviewId() == null || r.getReviewId().isEmpty()) {
            r.setReviewId("temp_" + java.util.UUID.randomUUID().toString());
        }

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

    public void getByRequest(String requestId, AppCallback<List<Review>> cb) {
        List<Review> cached = local.getByRequest(requestId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getByRequest(requestId, new AppCallback<List<Review>>() {
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
        List<Review> cached = local.getByReviewee(revieweeId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getAverageRating(revieweeId, cb);
        } else if (!cached.isEmpty()) {
            double sum = 0;
            for (Review r : cached) sum += r.getRating();
            cb.onSuccess(sum / cached.size());
        } else {
            cb.onError(new Exception("Không có mạng và không có cache"));
        }
    }
}
