package com.example.studenthousing;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

public class StudentHousingApp extends Application {
    private static StudentHousingApp instance;

    public static StudentHousingApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build();
            db.setFirestoreSettings(settings);
        } catch (Throwable ignored) {
            // Firestore settings must be configured before any Firestore queries are made
        }

        try {
            com.example.studenthousing.util.SyncManager syncManager = com.example.studenthousing.util.SyncManager.getInstance(this);
            syncManager.startAutoSync();
            syncManager.scheduleWorkManager();
        } catch (Throwable ignored) {}
    }
}
