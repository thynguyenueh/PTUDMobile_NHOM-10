package com.example.studenthousing.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.studenthousing.data.model.User;

public class SessionManager {
    private static final String PREF_NAME = "StudentHousingSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_FAMILY_NAME = "userFamilyName";
    private static final String KEY_USER_ROLE = "userRole";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context ctx) {
        pref = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        if (user != null) {
            editor.putString(KEY_USER_ID, user.getUserId());
            editor.putString(KEY_USER_PHONE, user.getPhone());
            editor.putString(KEY_USER_FAMILY_NAME, user.getFamilyName());
            editor.putString(KEY_USER_ROLE, user.getRole());
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, null);
    }

    public String getUserFamilyName() {
        return pref.getString(KEY_USER_FAMILY_NAME, null);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "PARENT");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    // Static convenience helpers
    public static void saveUid(Context ctx, String uid) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_IS_LOGGED_IN, true).putString(KEY_USER_ID, uid).apply();
    }

    public static String getUid(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_ID, null);
    }

    public static boolean isLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_IS_LOGGED_IN, false);
    }
}
