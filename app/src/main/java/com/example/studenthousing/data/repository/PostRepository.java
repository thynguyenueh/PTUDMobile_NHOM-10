package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PostRepository {
    private final Context ctx;
    private final PostDao local;
    private final PostRemote remote;

    public PostRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new PostDao(this.ctx);
        this.remote = new PostRemote();
    }

    public void getLatestPosts(int limit, AppCallback<List<AccommodationPost>> cb) {
        List<AccommodationPost> cached = local.getAll();

        if (NetworkUtils.isOnline(ctx)) {
            remote.getLatest(limit, new AppCallback<List<AccommodationPost>>() {
                @Override
                public void onSuccess(List<AccommodationPost> posts) {
                    local.replaceServerSnapshot(posts);
                    // Merge remote + pending local để không mất bài chưa sync
                    Map<String, AccommodationPost> merged = new LinkedHashMap<>();
                    if (posts != null) {
                        for (AccommodationPost p : posts) {
                            if (p.getPostId() != null) merged.put(p.getPostId(), p);
                        }
                    }
                    for (AccommodationPost p : local.getAll()) {
                        String id = p.getPostId();
                        if (id != null && !merged.containsKey(id)) {
                            merged.put(id, p);
                        }
                    }
                    cb.onSuccess(new ArrayList<>(merged.values()));
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

    public void getPostsByUserId(String userId, AppCallback<List<AccommodationPost>> cb) {
        List<AccommodationPost> cached = local.getByUserId(userId);
        if (cb != null) cb.onSuccess(cached);
    }

    public void getSavedPosts(AppCallback<List<AccommodationPost>> cb) {
        String userId = SessionManager.getUid(ctx);
        List<AccommodationPost> saved = local.getSavedPosts(userId);
        if (cb != null) cb.onSuccess(saved);
    }

    public void cacheRemotePostStatus(String postId, String status) {
        AccommodationPost cached = local.getById(postId);
        if (cached == null) return;
        cached.setStatus(status);
        cached.setUpdatedAt(System.currentTimeMillis());
        cached.setSyncStatus(Constants.SYNC_SYNCED);
        local.upsert(cached);
    }

    public void createPost(AccommodationPost p, AppCallback<String> cb) {
        String oldTempId = p.getPostId();
        if (oldTempId == null || oldTempId.isEmpty() || !oldTempId.startsWith("temp_")) {
            oldTempId = "temp_" + UUID.randomUUID();
            p.setPostId(oldTempId);
        }
        p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
        local.upsert(p);

        final String tempId = oldTempId;
        if (NetworkUtils.isOnline(ctx)) {
            remote.create(p, new AppCallback<String>() {
                @Override
                public void onSuccess(String postId) {
                    if (tempId != null && tempId.startsWith("temp_") && !tempId.equals(postId)) {
                        local.deleteById(tempId);
                    }
                    p.setPostId(postId);
                    p.setSyncStatus(Constants.SYNC_SYNCED);
                    local.upsert(p);
                    cb.onSuccess(postId);
                }

                @Override
                public void onError(Exception e) {
                    p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                    local.upsert(p);
                    cb.onSuccess(p.getPostId());
                }
            });
        } else {
            cb.onSuccess(p.getPostId());
        }
    }

    public void updatePost(AccommodationPost p, AppCallback<Void> cb) {
        if (p.getPostId() == null) {
            if (cb != null) cb.onError(new Exception("PostId null"));
            return;
        }

        p.setUpdatedAt(System.currentTimeMillis());
        if (!p.getPostId().startsWith("temp_")) {
            p.setSyncStatus(Constants.SYNC_PENDING_UPDATE);
        }
        local.upsert(p);

        if (NetworkUtils.isOnline(ctx) && !p.getPostId().startsWith("temp_")) {
            remote.update(p, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    p.setSyncStatus(Constants.SYNC_SYNCED);
                    local.upsert(p);
                    if (cb != null) cb.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(null);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(null);
        }
    }

    public void updatePostStatus(String postId, String status, AppCallback<Void> cb) {
        if (postId == null) {
            if (cb != null) cb.onError(new Exception("PostId null"));
            return;
        }
        AccommodationPost post = local.getById(postId);
        if (post != null) {
            post.setStatus(status);
            post.setUpdatedAt(System.currentTimeMillis());
            if (!postId.startsWith("temp_")) {
                post.setSyncStatus(Constants.SYNC_PENDING_UPDATE);
            }
            local.upsert(post);

            if (NetworkUtils.isOnline(ctx) && !postId.startsWith("temp_")) {
                remote.update(post, new AppCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        post.setSyncStatus(Constants.SYNC_SYNCED);
                        local.upsert(post);
                        if (cb != null) cb.onSuccess(null);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (cb != null) cb.onSuccess(null);
                    }
                });
            } else {
                if (cb != null) cb.onSuccess(null);
            }
        } else {
            if (cb != null) cb.onError(new Exception("Bài đăng không tồn tại trong cache"));
        }
    }

    public void deletePost(String postId, AppCallback<Void> cb) {
        if (postId == null) {
            if (cb != null) cb.onSuccess(null);
            return;
        }
        if (postId.startsWith("temp_")) {
            local.deleteById(postId);
            if (cb != null) cb.onSuccess(null);
            return;
        }

        AccommodationPost post = local.getById(postId);
        if (post != null) {
            post.setSyncStatus(Constants.SYNC_PENDING_DELETE);
            local.upsert(post);
        } else {
            local.deleteById(postId);
        }

        if (NetworkUtils.isOnline(ctx)) {
            remote.delete(postId, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    local.deleteById(postId);
                    if (cb != null) cb.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(null);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(null);
        }
    }

    public void getPostById(String postId, AppCallback<AccommodationPost> cb) {
        if (postId == null || postId.isEmpty()) {
            if (cb != null) cb.onError(new Exception("ID không hợp lệ"));
            return;
        }

        AccommodationPost cached = local.getById(postId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getById(postId, new AppCallback<AccommodationPost>() {
                @Override
                public void onSuccess(AccommodationPost post) {
                    if (post != null) {
                        post.setSyncStatus(Constants.SYNC_SYNCED);
                        local.upsert(post);
                        if (cb != null) cb.onSuccess(post);
                    } else if (cached != null) {
                        if (cb != null) cb.onSuccess(cached);
                    } else if (cb != null) {
                        cb.onError(new Exception("Không tìm thấy bài đăng"));
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (cached != null) {
                        if (cb != null) cb.onSuccess(cached);
                    } else if (cb != null) {
                        cb.onError(e);
                    }
                }
            });
        } else if (cached != null) {
            if (cb != null) cb.onSuccess(cached);
        } else if (cb != null) {
            cb.onError(new Exception("Không có mạng và bài đăng không có trong cache"));
        }
    }

    public List<AccommodationPost> getCachedPosts() {
        return local.getAll();
    }

    public boolean isSaved(String postId) {
        String userId = SessionManager.getUid(ctx);
        return local.isSaved(userId, postId);
    }

    public void savePost(String postId, AppCallback<Void> cb) {
        String userId = SessionManager.getUid(ctx);
        if (userId != null) {
            local.savePost(userId, postId);
            if (NetworkUtils.isOnline(ctx)) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("savedPosts", FieldValue.arrayUnion(postId));
            }
            if (cb != null) cb.onSuccess(null);
        } else {
            if (cb != null) cb.onError(new Exception("Chưa đăng nhập"));
        }
    }

    public void unsavePost(String postId, AppCallback<Void> cb) {
        String userId = SessionManager.getUid(ctx);
        if (userId != null) {
            local.unsavePost(userId, postId);
            if (NetworkUtils.isOnline(ctx)) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("savedPosts", FieldValue.arrayRemove(postId));
            }
            if (cb != null) cb.onSuccess(null);
        } else {
            if (cb != null) cb.onError(null);
        }
    }

    public void toggleFavorite(String postId, AppCallback<Boolean> cb) {
        String userId = SessionManager.getUid(ctx);
        if (userId == null) {
            if (cb != null) cb.onError(new Exception("Chưa đăng nhập"));
            return;
        }
        boolean currentlySaved = local.isSaved(userId, postId);
        if (currentlySaved) {
            unsavePost(postId, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    if (cb != null) cb.onSuccess(false);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(false);
                }
            });
        } else {
            savePost(postId, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    if (cb != null) cb.onSuccess(true);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(true);
                }
            });
        }
    }
}