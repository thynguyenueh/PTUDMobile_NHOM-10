package com.example.studenthousing.data.repository;

import android.content.Context;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.remote.UserRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;

public class UserRepository {
    private final Context ctx;
    private final UserRemote remote;
    private final SessionManager session;

    public UserRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.remote = new UserRemote();
        this.session = new SessionManager(this.ctx);
    }

    public void saveUserProfile(User user, AppCallback<Void> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.saveUser(user, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    session.createLoginSession(user);
                    cb.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    cb.onError(e);
                }
            });
        } else {
            cb.onError(new Exception("Cần kết nối mạng để lưu thông tin tài khoản"));
        }
    }

    public void getUserProfile(String userId, AppCallback<User> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.getUser(userId, cb);
        } else {
            cb.onError(new Exception("Không có kết nối mạng"));
        }
    }

    public SessionManager getSession() {
        return session;
    }
}
