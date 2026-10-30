package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.util.AddressFingerprintUtil;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.VerificationBadge;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostRepository {
    private final Context ctx;
    private final PostDao local;
    private final VerifiedAddressDao addressDao;
    private final PostRemote remote;

    public PostRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new PostDao(this.ctx);
        this.addressDao = new VerifiedAddressDao(this.ctx);
        this.remote = new PostRemote();
    }

    public void getLatestPosts(int limit, AppCallback<List<Post>> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.getLatest(limit, new AppCallback<List<Post>>() {
                @Override
                public void onSuccess(List<Post> posts) {
                    if (posts != null && !posts.isEmpty()) {
                        local.replaceServerSnapshot(posts);
                    }
                    List<Post> combined = local.getAll();
                    cb.onSuccess(combined != null ? combined : new java.util.ArrayList<>());
                }

                @Override
                public void onError(Exception e) {
                    List<Post> cached = local.getAll();
                    if (cached != null && !cached.isEmpty()) {
                        cb.onSuccess(cached);
                    } else {
                        cb.onError(e);
                    }
                }
            });
        } else {
            List<Post> cached = local.getAll();
            if (cached != null && !cached.isEmpty()) {
                cb.onSuccess(cached);
            } else {
                cb.onError(new Exception("Không có mạng và không có cache"));
            }
        }
    }

    public void getPostsByUserId(String userId, AppCallback<List<Post>> cb) {
        List<Post> cached = local.getByUserId(userId);
        if (cb != null) cb.onSuccess(cached);
    }

    public void getSavedPosts(AppCallback<List<Post>> cb) {
        String userId = SessionManager.getUid(ctx);
        List<Post> saved = local.getSavedPosts(userId);
        if (cb != null) cb.onSuccess(saved);
    }

    public void cacheRemotePostStatus(String postId, String status) {
        Post cached = local.getById(postId);
        if (cached == null) return;
        cached.setStatus(status);
        cached.setUpdatedAt(System.currentTimeMillis());
        cached.setSyncStatus(Constants.SYNC_SYNCED);
        local.upsert(cached);
    }

    public void createPost(Post p, AppCallback<String> cb) {
        String oldTempId = p.getPostId();
        if (oldTempId == null || oldTempId.isEmpty() || !oldTempId.startsWith("temp_")) {
            oldTempId = "temp_" + System.currentTimeMillis();
            p.setPostId(oldTempId);
        }

        if (p.getAddressFingerprint() == null || p.getAddressFingerprint().isEmpty()) {
            String fp = AddressFingerprintUtil.build(
                    p.getCityCode(),
                    p.getWardCode(),
                    p.getStreetName() != null ? p.getStreetName() : "",
                    p.getHouseNumber() != null ? p.getHouseNumber() : ""
            );
            p.setAddressFingerprint(fp);
        }

        VerifiedAddress verifiedAddr = addressDao.findByFingerprint(p.getAddressFingerprint());
        if (verifiedAddr != null) {
            if (p.getUserId() != null && p.getUserId().equals(verifiedAddr.getOwnerUserId())) {
                p.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
            } else {
                p.setAddressVerificationStatus(Constants.ADDR_DISPUTED);
            }
        } else {
            p.setAddressVerificationStatus(Constants.ADDR_UNVERIFIED);
        }

        p.setVerificationBadge(VerificationBadge.addressBadgeText(p.getAddressVerificationStatus()));
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
                    if (cb != null) cb.onError(e);
                }
            });
        } else {
            cb.onSuccess(p.getPostId());
        }
    }

    public void updatePost(Post p, AppCallback<Void> cb) {
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
        Post post = local.getById(postId);
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

        Post post = local.getById(postId);
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

    public void getPostById(String postId, AppCallback<Post> cb) {
        if (postId == null || postId.isEmpty()) {
            if (cb != null) cb.onError(new Exception("ID không hợp lệ"));
            return;
        }

        Post cached = local.getById(postId);

        if (NetworkUtils.isOnline(ctx)) {
            FirebaseFirestore.getInstance()
                    .collection("accommodation_posts")
                    .document(postId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                post.setSyncStatus(Constants.SYNC_SYNCED);
                                local.upsert(post);
                            }
                            if (cb != null) cb.onSuccess(post);
                        } else if (cached != null) {
                            if (cb != null) cb.onSuccess(cached);
                        } else if (cb != null) {
                            cb.onError(new Exception("Không tìm thấy bài đăng"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (cached != null) {
                            if (cb != null) cb.onSuccess(cached);
                        } else if (cb != null) {
                            cb.onError(e);
                        }
                    });
        } else if (cached != null) {
            if (cb != null) cb.onSuccess(cached);
        } else if (cb != null) {
            cb.onError(new Exception("Không có mạng và bài đăng không có trong cache"));
        }
    }

    public List<Post> getCachedPosts() {
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
