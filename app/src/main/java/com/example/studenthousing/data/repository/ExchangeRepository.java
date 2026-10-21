package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.remote.ExchangeRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
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

    public void create(ExchangeRequest r, AppCallback<String> cb) {
        if (r.getCreatedAt() == 0) r.setCreatedAt(System.currentTimeMillis());
        if (r.getRequestId() == null || r.getRequestId().isEmpty()) {
            r.setRequestId("temp_" + java.util.UUID.randomUUID().toString());
        }

        if (!NetworkUtils.isOnline(ctx)) {
            r.setSyncStatus("PENDING_CREATE");
            local.upsert(r);
            if (cb != null) cb.onSuccess(r.getRequestId());
            return;
        }

        remote.create(r, new AppCallback<String>() {
            @Override public void onSuccess(String id) {
                r.setRequestId(id);
                r.setSyncStatus("SYNCED");
                local.upsert(r);
                if (cb != null) cb.onSuccess(id);
            }
            @Override public void onError(Exception e) {
                r.setSyncStatus("PENDING_CREATE");
                local.upsert(r);
                if (cb != null) cb.onSuccess(r.getRequestId());
            }
        });
    }

    public void updateStatus(String requestId, String status, AppCallback<Void> cb) {
        if (!NetworkUtils.isOnline(ctx)) {
            if (cb != null) cb.onError(new Exception("Bạn cần có kết nối mạng để phản hồi đề xuất đổi trọ."));
            return;
        }

        remote.updateStatus(requestId, status, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void d) {
                ExchangeRequest r = local.getById(requestId);
                if (r != null) {
                    r.setStatus(status);
                    r.setRespondedAt(System.currentTimeMillis());
                    r.setSyncStatus("SYNCED");
                    local.upsert(r);

                    if (Constants.REQ_COMPLETED.equals(status) || "COMPLETED".equals(status)) {
                        PostRepository postRepo = new PostRepository(ctx);
                        if (r.getSenderPostId() != null) {
                            postRepo.updatePostStatus(r.getSenderPostId(), Constants.POST_EXCHANGED, null);
                        }
                        if (r.getReceiverPostId() != null) {
                            postRepo.updatePostStatus(r.getReceiverPostId(), Constants.POST_EXCHANGED, null);
                        }
                    }
                }
                if (cb != null) cb.onSuccess(null);
            }

            @Override
            public void onError(Exception e) {
                if (cb != null) cb.onError(e);
            }
        });
    }

    public void getById(String requestId, AppCallback<ExchangeRequest> cb) {
        ExchangeRequest cached = local.getById(requestId);
        if (cached != null) {
            cb.onSuccess(cached);
        } else if (cb != null) {
            cb.onError(new Exception("Không tìm thấy đề xuất đổi trọ"));
        }
    }

    public void getAllForUser(String userId, AppCallback<List<ExchangeRequest>> cb) {
        if (userId == null || userId.isEmpty()) {
            if (cb != null) cb.onError(new Exception("User ID không hợp lệ"));
            return;
        }
        getBySender(userId, new AppCallback<List<ExchangeRequest>>() {
            @Override public void onSuccess(List<ExchangeRequest> sent) {
                getByReceiver(userId, new AppCallback<List<ExchangeRequest>>() {
                    @Override public void onSuccess(List<ExchangeRequest> received) {
                        java.util.Map<String, ExchangeRequest> merged = new java.util.LinkedHashMap<>();
                        if (sent != null) for (ExchangeRequest r : sent) merged.put(r.getRequestId(), r);
                        if (received != null) for (ExchangeRequest r : received) merged.put(r.getRequestId(), r);
                        if (cb != null) cb.onSuccess(new java.util.ArrayList<>(merged.values()));
                    }
                    @Override public void onError(Exception e) { if (cb != null) cb.onError(e); }
                });
            }
            @Override public void onError(Exception e) { if (cb != null) cb.onError(e); }
        });
    }

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
