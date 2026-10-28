package com.example.studenthousing.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.model.ExchangeRequest;

import java.util.List;

public class AutoTransitionReceiver extends BroadcastReceiver {
    private static final String TAG = "AutoTransitionReceiver";
    private static final String CHANNEL_ID = "channel_auto_transition";
    private static final String CHANNEL_NAME = "Thông báo tự động đổi nhà";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AutoTransitionReceiver triggered by AlarmManager");

        Context appCtx = context.getApplicationContext();
        String currentUserId = SessionManager.getUid(appCtx);
        if (currentUserId == null) return;

        ExchangeDao exchangeDao = new ExchangeDao(appCtx);
        List<ExchangeRequest> requests = exchangeDao.getAllForUser(currentUserId);

        if (requests == null || requests.isEmpty()) return;

        ExchangeStatusChecker.checkAndApplyExpirations(appCtx, requests, currentUserId, null);

        createNotificationChannel(appCtx);
        showTransitionNotification(appCtx, "Cập nhật trạng thái thỏa thuận", "Ứng dụng đã kiểm tra và cập nhật trạng thái tự động cho các thỏa thuận của bạn.");
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Kênh thông báo tự động chuyển trạng thái thỏa thuận đổi nhà");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void showTransitionNotification(Context context, String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission missing: " + e.getMessage());
        }
    }
}
