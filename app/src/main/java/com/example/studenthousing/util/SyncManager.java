package com.example.studenthousing.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.remote.ExchangeRemote;
import com.example.studenthousing.data.remote.ReviewRemote;

import java.util.List;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context ctx;
    private final ExchangeDao exchangeDao;
    private final ReviewDao reviewDao;
    private final ExchangeRemote exchangeRemote;
    private final ReviewRemote reviewRemote;

    public SyncManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.exchangeDao = new ExchangeDao(this.ctx);
        this.reviewDao = new ReviewDao(this.ctx);
        this.exchangeRemote = new ExchangeRemote();
        this.reviewRemote = new ReviewRemote();
    }

    public void startAutoSync() {
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        try {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "Có mạng — bắt đầu tự động đồng bộ Firestore <-> SQLite");
                    syncAll();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đăng ký NetworkCallback: " + e.getMessage());
        }
    }

    public void syncAll() {
        syncExchanges();
        syncReviews();
    }

    private void syncExchanges() {
        List<ExchangeRequest> senderList = exchangeDao.getBySender("current_user");
        if (senderList != null) {
            for (ExchangeRequest r : senderList) {
                if (r.getRequestId() != null && r.getRequestId().startsWith("temp_")) {
                    syncExchange(r, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            Log.d(TAG, "Đã đồng bộ ExchangeRequest lên Firestore: " + id);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Thất bại đồng bộ ExchangeRequest: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }

    private void syncReviews() {
        List<Review> list = reviewDao.getByReviewee("current_user");
        if (list != null) {
            for (Review r : list) {
                if (r.getReviewId() != null && r.getReviewId().startsWith("temp_")) {
                    syncReview(r, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            Log.d(TAG, "Đã đồng bộ Review lên Firestore: " + id);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Thất bại đồng bộ Review: " + e.getMessage());
                        }
                    });
                }
            }
        }
    }

    // Sync 1 request cụ thể (dùng khi cần force sync)
    public void syncExchange(ExchangeRequest r, AppCallback<String> cb) {
        exchangeRemote.create(r, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                r.setRequestId(id);
                exchangeDao.upsert(r);
                cb.onSuccess(id);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }

    public void syncReview(Review r, AppCallback<String> cb) {
        reviewRemote.create(r, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                r.setReviewId(id);
                reviewDao.upsert(r);
                cb.onSuccess(id);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }
}
