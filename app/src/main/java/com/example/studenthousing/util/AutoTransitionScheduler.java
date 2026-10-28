package com.example.studenthousing.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoTransitionScheduler {
    private static final String TAG = "AutoTransitionScheduler";
    private static final int REQUEST_CODE = 88812;

    public static void schedule(Context context) {
        try {
            Context appCtx = context.getApplicationContext();
            AlarmManager alarmManager = (AlarmManager) appCtx.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(appCtx, AutoTransitionReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    appCtx,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long intervalMs = AlarmManager.INTERVAL_HALF_DAY; // Every 12 hours
            long triggerAtMs = System.currentTimeMillis() + intervalMs;

            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    intervalMs,
                    pendingIntent
            );
            Log.d(TAG, "Scheduled AutoTransition AlarmManager successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling AlarmManager: " + e.getMessage());
        }
    }
}
