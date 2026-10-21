package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
            boolean success = SyncManager.getInstance(getApplicationContext()).syncAll();
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
