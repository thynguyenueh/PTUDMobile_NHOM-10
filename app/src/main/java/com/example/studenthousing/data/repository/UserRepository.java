package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.remote.UserRemote;
import com.example.studenthousing.util.AppCallback;
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

        local.upsert(user);
        session.createLoginSession(user);

        if (NetworkUtils.isOnline(ctx)) {
            remote.saveUser(user, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    if (cb != null) cb.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    // Đã lưu SQLite và Session
                    if (cb != null) cb.onSuccess(null);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(null);
        }
    }

    public void getUserProfile(String userId, AppCallback<User> cb) {
        User cached = local.getById(userId);
        if (cached != null && cb != null) {
            cb.onSuccess(cached);
        }

        if (NetworkUtils.isOnline(ctx)) {
            remote.getUser(userId, new AppCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        local.upsert(user);
                        if (cb != null) cb.onSuccess(user);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (cached == null && cb != null) {
                        cb.onError(e);
                    }
                }
            });
        } else if (cached == null && cb != null) {
            cb.onError(new Exception("Không có kết nối mạng và không có cache"));
        }
    }

    public SessionManager getSession() {
        return session;
    }
}
