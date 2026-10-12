package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.remote.ExchangeRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ExchangeRepository {
    private final Context ctx;
    private final ExchangeDao local;
    private final ExchangeRemote remote;

    public ExchangeRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new ExchangeDao(this.ctx);
        this.remote = new ExchangeRemote();
    }

    // Tạo request
    public void create(ExchangeRequest r, AppCallback<String> cb) {
        if (r.getCreatedAt() == 0) r.setCreatedAt(System.currentTimeMillis());

        if (NetworkUtils.isOnline(ctx)) {
            remote.create(r, new AppCallback<String>() {
                @Override
                public void onSuccess(String id) {
                    r.setRequestId(id);
                    local.upsert(r);
                    cb.onSuccess(id);
                }

                @Override
                public void onError(Exception e) {
                    // Vẫn lưu local để retry sau
                    String tempId = "temp_" + System.currentTimeMillis();
                    r.setRequestId(tempId);
                    local.upsert(r);
                    cb.onError(e);
                }
            });
        } else {
            // Offline → lưu local với temp ID
            String tempId = "temp_" + System.currentTimeMillis();
            r.setRequestId(tempId);
            local.upsert(r);
            cb.onError(new Exception("Offline — đã lưu local, sẽ sync sau"));
        }
    }

    // Update status (approve / reject / cancel)
    public void updateStatus(String requestId, String status, AppCallback<Void> cb) {
        if (NetworkUtils.isOnline(ctx)) {
            remote.updateStatus(requestId, status, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void d) {
                    // Update local
                    ExchangeRequest r = local.getById(requestId);
                    if (r != null) {
                        r.setStatus(status);
                        r.setRespondedAt(System.currentTimeMillis());
                        local.upsert(r);
                    }
                    cb.onSuccess(null);
                }

                @Override
                public void onError(Exception e) {
                    cb.onError(e);
                }
            });
        } else {
            // Update local, đợi sync
            ExchangeRequest r = local.getById(requestId);
            if (r != null) {
                r.setStatus(status);
                r.setRespondedAt(System.currentTimeMillis());
                local.upsert(r);
            }
            cb.onError(new Exception("Offline — đã update local"));
        }
    }

    // Đọc danh sách gửi
    public void getBySender(String senderId, AppCallback<List<ExchangeRequest>> cb) {
        List<ExchangeRequest> cached = local.getBySender(senderId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getBySender(senderId, new AppCallback<List<ExchangeRequest>>() {
                @Override
                public void onSuccess(List<ExchangeRequest> list) {
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

    public void getByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        List<ExchangeRequest> cached = local.getByReceiver(receiverId);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getByReceiver(receiverId, new AppCallback<List<ExchangeRequest>>() {
                @Override
                public void onSuccess(List<ExchangeRequest> list) {
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

    // REALTIME — user theo dõi request gửi
    public ListenerRegistration listenBySender(String senderId, AppCallback<List<ExchangeRequest>> cb) {
        List<ExchangeRequest> cached = local.getBySender(senderId);
        if (!cached.isEmpty()) cb.onSuccess(cached);

        return remote.listenBySender(senderId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                local.upsertAll(list);
                cb.onSuccess(list);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }

    // REALTIME — user theo dõi request nhận
    public ListenerRegistration listenByReceiver(String receiverId, AppCallback<List<ExchangeRequest>> cb) {
        List<ExchangeRequest> cached = local.getByReceiver(receiverId);
        if (!cached.isEmpty()) cb.onSuccess(cached);

        return remote.listenByReceiver(receiverId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                local.upsertAll(list);
                cb.onSuccess(list);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }

    // REALTIME — theo dõi 1 request cụ thể (để update status realtime)
    public ListenerRegistration listenById(String requestId, AppCallback<ExchangeRequest> cb) {
        ExchangeRequest cached = local.getById(requestId);
        if (cached != null) cb.onSuccess(cached);

        return remote.listenById(requestId, new AppCallback<ExchangeRequest>() {
            @Override
            public void onSuccess(ExchangeRequest r) {
                if (r != null) local.upsert(r);
                cb.onSuccess(r);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }
}
