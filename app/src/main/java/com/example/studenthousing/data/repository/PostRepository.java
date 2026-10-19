package com.example.studenthousing.data.repository;

import android.content.Context;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import java.util.List;

public class PostRepository {
    private final Context ctx;
    private final PostDao local;
    private final PostRemote remote;

    public PostRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new PostDao(this.ctx);
        this.remote = new PostRemote();
    }

    // Đọc danh sách: Firestore nếu online (lưu cache), fallback SQLite khi offline/lỗi
    public void getLatestPosts(int limit, AppCallback<List<AccommodationPost>> cb) {
        List<AccommodationPost> cached = local.getAll();

        if (NetworkUtils.isOnline(ctx)) {
            remote.getLatest(limit, new AppCallback<List<AccommodationPost>>() {
                @Override
                public void onSuccess(List<AccommodationPost> posts) {
                    local.upsertAll(posts);   // Cache lại
                    cb.onSuccess(posts);      // Trả dữ liệu mới nhất
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

    // Tạo bài
    public void createPost(AccommodationPost p, AppCallback<String> cb) {
        local.upsert(p); // Lưu cục bộ ngay lập tức để có thể tìm kiếm ngay
        if (NetworkUtils.isOnline(ctx)) {
            remote.create(p, new AppCallback<String>() {
                @Override
                public void onSuccess(String postId) {
                    p.setPostId(postId);
                    local.upsert(p);  // Cache
                    cb.onSuccess(postId);
                }

                @Override
                public void onError(Exception e) {
                    cb.onSuccess(p.getPostId());
                }
            });
        } else {
            cb.onSuccess(p.getPostId());
        }
    }

    public List<AccommodationPost> getCachedPosts() {
        return local.getAll();
    }
}
