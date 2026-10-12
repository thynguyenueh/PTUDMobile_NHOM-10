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

    // Tạo review
    public void create(Review r, AppCallback<String> cb) {
        if (r.getCreatedAt() == 0) r.setCreatedAt(System.currentTimeMillis());

        if (NetworkUtils.isOnline(ctx)) {
            remote.create(r, new AppCallback<String>() {
                @Override
                public void onSuccess(String id) {
                    r.setReviewId(id);
                    local.upsert(r);
                    cb.onSuccess(id);
                }

                @Override
                public void onError(Exception e) {
                    // Vẫn cache local
                    String tempId = "temp_" + System.currentTimeMillis();
                    r.setReviewId(tempId);
                    local.upsert(r);
                    cb.onError(e);
                }
            });
        } else {
            String tempId = "temp_" + System.currentTimeMillis();
            r.setReviewId(tempId);
            local.upsert(r);
            cb.onError(new Exception("Offline — đã lưu local"));
        }
    }

    // Đọc review theo reviewee
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

    // Đọc review theo request
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

    // Điểm trung bình
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
