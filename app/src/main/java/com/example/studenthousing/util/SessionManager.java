package com.example.studenthousing.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "StudentHousingSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_FAMILY_NAME = "userFamilyName";
    private static final String KEY_USER_AVATAR_URL = "userAvatarUrl";

    private final Context context;
    private final SharedPreferences pref;

    public SessionManager(Context ctx) {
        context = ctx.getApplicationContext();
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(String userId) {
        pref.edit().putBoolean(KEY_IS_LOGGED_IN, userId != null && !userId.isEmpty())
                .putString(KEY_USER_ID, userId).apply();
    }

    public void createLoginSession(com.example.studenthousing.data.model.User user) {
        createLoginSession(user == null ? null : user.getUserId());
        if (user != null) {
            pref.edit().putString(KEY_USER_PHONE, user.getPhone())
                    .putString(KEY_USER_FAMILY_NAME, user.getFamilyName())
                    .putString(KEY_USER_AVATAR_URL, user.getAvatarUrl()).apply();
        }
    }

    public String getUserPhone() { return pref.getString(KEY_USER_PHONE, null); }
    public String getUserFamilyName() { return pref.getString(KEY_USER_FAMILY_NAME, null); }
    public String getAvatarUrl() { return pref.getString(KEY_USER_AVATAR_URL, null); }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != null;
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public void logout() {
        pref.edit().clear().apply();
        try {
            android.database.sqlite.SQLiteDatabase db =
                    com.example.studenthousing.data.local.DatabaseHelper.getInstance(context).getWritableDatabase();
            String whereSynced = com.example.studenthousing.data.local.DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " +
                    com.example.studenthousing.data.local.DatabaseHelper.COL_SYNC_STATUS + " = 'SYNCED'";
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_POSTS, whereSynced, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_MESSAGES, whereSynced, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_EXCHANGE, whereSynced, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_REVIEWS, whereSynced, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_CHAT_ROOMS, null, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_USERS, null, null);
            db.delete(com.example.studenthousing.data.local.DatabaseHelper.TBL_SAVED_HOUSES, null, null);
        } catch (Exception ignored) {}
    }

    public static void saveUid(Context ctx, String uid) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_IS_LOGGED_IN, uid != null && !uid.isEmpty())
                .putString(KEY_USER_ID, uid).apply();
    }

    public static String getUid(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String uid = sp.getString(KEY_USER_ID, null);
        return (uid != null && !uid.isEmpty()) ? uid : null;
    }

    public static boolean isLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_IS_LOGGED_IN, false);
    }
}
