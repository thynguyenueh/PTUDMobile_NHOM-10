package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.remote.UserRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;

public class UserRepository {
    private final Context ctx;
    private final UserRemote remote;
    private final UserDao local;
    private final SessionManager session;

    public UserRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.remote = new UserRemote();
        this.local = new UserDao(this.ctx);
        this.session = new SessionManager(this.ctx);
    }

    public void saveUserProfile(User user, AppCallback<Void> cb) {
        if (user == null) {
            if (cb != null) cb.onError(new Exception("User null"));
            return;
        }

        recomputeFullyVerified(user);

        if (!NetworkUtils.isOnline(ctx)) {
            local.upsert(user);
            session.createLoginSession(user);
            if (cb != null) cb.onSuccess(null);
            return;
        }

        remote.saveUser(user, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                local.upsert(user);
                session.createLoginSession(user);
                if (cb != null) cb.onSuccess(null);
            }

            @Override
            public void onError(Exception e) {
                if (cb != null) cb.onError(e);
            }
        });
    }

    public void markIdentityVerified(String userId, String identityType, AppCallback<Void> cb) {
        User u = getCachedUser(userId);
        if (u != null) {
            u.setIdentityVerified(true);
            u.setIdentityType(identityType != null ? identityType : Constants.IDENTITY_STUDENT);
            u.setIdentityVerifiedAt(System.currentTimeMillis());
            u.setVerified(true);
            saveUserProfile(u, cb);
        } else if (cb != null) {
            cb.onError(new Exception("User not found in cache"));
        }
    }

    public void markResidenceVerified(String userId, AppCallback<Void> cb) {
        User u = getCachedUser(userId);
        if (u != null) {
            u.setResidenceVerified(true);
            u.setResidenceVerifiedAt(System.currentTimeMillis());
            saveUserProfile(u, cb);
        } else if (cb != null) {
            cb.onError(new Exception("User not found in cache"));
        }
    }

    public void recomputeFullyVerified(User u) {
        if (u == null) return;
        boolean fully = u.isIdentityVerified() && u.isResidenceVerified();
        u.setFullyVerified(fully);
    }

    public void upsertAuthProfile(User partial, AppCallback<User> cb) {
        if (partial == null || partial.getUserId() == null) {
            if (cb != null) cb.onError(new Exception("User null"));
            return;
        }
        User localExisting = local.getById(partial.getUserId());
        if (localExisting != null) {
            merge(localExisting, partial);
            saveUserProfile(localExisting, wrap(cb, localExisting));
            return;
        }
        remote.getUser(partial.getUserId(), new AppCallback<User>() {
            @Override public void onSuccess(User remoteExisting) {
                User base = remoteExisting != null ? remoteExisting : partial;
                merge(base, partial);
                saveUserProfile(base, wrap(cb, base));
            }
            @Override public void onError(Exception e) {
                if (!NetworkUtils.isOnline(ctx)) saveUserProfile(partial, wrap(cb, partial));
                else if (cb != null) cb.onError(e);
            }
        });
    }

    private void merge(User target, User src) {
        if (src.getEmail() != null && !src.getEmail().isEmpty()) target.setEmail(src.getEmail());
        if (src.getAvatarUrl() != null && !src.getAvatarUrl().isEmpty()) target.setAvatarUrl(src.getAvatarUrl());
        if ((target.getFamilyName() == null || target.getFamilyName().isEmpty()) && src.getFamilyName() != null)
            target.setFamilyName(src.getFamilyName());
        if (src.getAuthProviders() != null && !src.getAuthProviders().isEmpty())
            target.setAuthProviders(src.getAuthProviders());
        target.setUpdatedAt(System.currentTimeMillis());
    }

    private AppCallback<Void> wrap(AppCallback<User> cb, User user) {
        return new AppCallback<Void>() {
            @Override public void onSuccess(Void d) { if (cb != null) cb.onSuccess(user); }
            @Override public void onError(Exception e) { if (cb != null) cb.onError(e); }
        };
    }

    public User getCachedUser(String userId) {
        return userId == null ? null : local.getById(userId);
    }

    public void getCachedUserProfile(String userId, AppCallback<User> cb) {
        User cached = userId == null ? null : local.getById(userId);
        if (cb == null) return;
        if (cached != null) cb.onSuccess(cached);
        else cb.onError(new Exception("Không có hồ sơ trong cache"));
    }

    public void getUserProfile(String userId, AppCallback<User> cb) {
        User cached = local.getById(userId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getUser(userId, new AppCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        local.upsert(user);
                        if (cb != null) cb.onSuccess(user);
                    } else if (cached != null && cb != null) {
                        cb.onSuccess(cached);
                    } else if (cb != null) {
                        cb.onError(new Exception("Không tìm thấy thông tin người dùng"));
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (cached != null && cb != null) {
                        cb.onSuccess(cached);
                    } else if (cb != null) {
                        cb.onError(e);
                    }
                }
            });
        } else if (cached != null && cb != null) {
            cb.onSuccess(cached);
        } else if (cb != null) {
            cb.onError(new Exception("Không có kết nối mạng và không có cache"));
        }
    }

    public SessionManager getSession() {
        return session;
    }
}
