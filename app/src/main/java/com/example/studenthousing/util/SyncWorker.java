package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.model.ExchangeRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * LƯU Ý: Đây là kiểm tra phía client (chạy qua SyncWorker), không chạy khi app không mở hoàn toàn.
 */
public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker đang chạy đồng bộ nền...");
        try {
            Context ctx = getApplicationContext();
            ExchangeDao exchangeDao = new ExchangeDao(ctx);
            String currentUserId = SessionManager.getUid(ctx);
            List<ExchangeRequest> allRequests = currentUserId != null ? exchangeDao.getAllForUser(currentUserId) : new ArrayList<>();
            ExchangeStatusChecker.checkAndApplyExpirations(ctx, allRequests, currentUserId, null);

            boolean success = SyncManager.getInstance(ctx).syncAll();
            if (!success) {
                Log.w(TAG, "SyncWorker có tác vụ thất bại, yêu cầu retry...");
                return Result.retry();
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "SyncWorker lỗi: " + e.getMessage());
            return Result.retry();
        }
    }
}
