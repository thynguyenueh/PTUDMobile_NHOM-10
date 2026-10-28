package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.User;

public class UserDao {
    private final DatabaseHelper dbHelper;

    public UserDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(User u) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_USERS, null, toValues(u),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User getById(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_USERS, null,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{userId}, null, null, null);
        if (c.moveToFirst()) {
            User u = cursorTo(c);
            c.close();
            return u;
        }
        c.close();
        return null;
    }

    public User getByPhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TBL_USERS, null,
                DatabaseHelper.COL_PHONE + "=?",
                new String[]{phone}, null, null, null)) {
            return c.moveToFirst() ? cursorTo(c) : null;
        }
    }

    private ContentValues toValues(User u) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_USER_ID, u.getUserId());
        v.put(DatabaseHelper.COL_FAMILY_NAME, u.getFamilyName());
        v.put(DatabaseHelper.COL_PHONE, u.getPhone());
        v.put(DatabaseHelper.COL_PASSWORD, u.getPassword());
        v.put(DatabaseHelper.COL_AVATAR_URL, u.getAvatarUrl());
        v.put(DatabaseHelper.COL_IDENTITY_TYPE, u.getIdentityType());
        v.put(DatabaseHelper.COL_IDENTITY_VERIFIED, u.isIdentityVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_IDENTITY_VERIFIED_AT, u.getIdentityVerifiedAt());
        v.put(DatabaseHelper.COL_UNIVERSITY_CODE, u.getUniversityCode());
        v.put(DatabaseHelper.COL_UNIVERSITY_NAME, u.getUniversityName());
        v.put(DatabaseHelper.COL_CITIZEN_ID_HASH, u.getCitizenIdHash());
        v.put(DatabaseHelper.COL_RESIDENCE_VERIFIED, u.isResidenceVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_RESIDENCE_VERIFIED_AT, u.getResidenceVerifiedAt());
        v.put(DatabaseHelper.COL_FULLY_VERIFIED, u.isFullyVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_PERMANENT_ADDRESS, u.getPermanentAddress());
        v.put(DatabaseHelper.COL_PERMANENT_CITY_CODE, u.getPermanentCityCode());
        v.put(DatabaseHelper.COL_PERMANENT_WARD_CODE, u.getPermanentWardCode());
        v.put(DatabaseHelper.COL_PERMANENT_CITY_NAME, u.getPermanentCityName());
        v.put(DatabaseHelper.COL_PERMANENT_WARD_NAME, u.getPermanentWardName());
        v.put(DatabaseHelper.COL_PHONE_VERIFIED, u.isPhoneVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_IS_VERIFIED, u.isVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_CREATED_AT, u.getCreatedAt());
        v.put(DatabaseHelper.COL_UPDATED_AT, u.getUpdatedAt() > 0 ? u.getUpdatedAt() : System.currentTimeMillis());
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private User cursorTo(Cursor c) {
        User u = new User();
        u.setUserId(getStringOrNull(c, DatabaseHelper.COL_USER_ID));
        u.setFamilyName(getStringOrNull(c, DatabaseHelper.COL_FAMILY_NAME));
        u.setPhone(getStringOrNull(c, DatabaseHelper.COL_PHONE));
        u.setPassword(getStringOrNull(c, DatabaseHelper.COL_PASSWORD));
        u.setAvatarUrl(getStringOrNull(c, DatabaseHelper.COL_AVATAR_URL));
        u.setIdentityType(getStringOrNull(c, DatabaseHelper.COL_IDENTITY_TYPE));
        u.setIdentityVerified(getIntAsBool(c, DatabaseHelper.COL_IDENTITY_VERIFIED));
        u.setIdentityVerifiedAt(getLongOrDefault(c, DatabaseHelper.COL_IDENTITY_VERIFIED_AT, 0L));
        u.setUniversityCode(getStringOrNull(c, DatabaseHelper.COL_UNIVERSITY_CODE));
        u.setUniversityName(getStringOrNull(c, DatabaseHelper.COL_UNIVERSITY_NAME));
        u.setCitizenIdHash(getStringOrNull(c, DatabaseHelper.COL_CITIZEN_ID_HASH));
        u.setResidenceVerified(getIntAsBool(c, DatabaseHelper.COL_RESIDENCE_VERIFIED));
        u.setResidenceVerifiedAt(getLongOrDefault(c, DatabaseHelper.COL_RESIDENCE_VERIFIED_AT, 0L));
        u.setFullyVerified(getIntAsBool(c, DatabaseHelper.COL_FULLY_VERIFIED));
        u.setPermanentAddress(getStringOrNull(c, DatabaseHelper.COL_PERMANENT_ADDRESS));
        u.setPermanentCityCode(getIntOrDefault(c, DatabaseHelper.COL_PERMANENT_CITY_CODE, 0));
        u.setPermanentWardCode(getIntOrDefault(c, DatabaseHelper.COL_PERMANENT_WARD_CODE, 0));
        u.setPermanentCityName(getStringOrNull(c, DatabaseHelper.COL_PERMANENT_CITY_NAME));
        u.setPermanentWardName(getStringOrNull(c, DatabaseHelper.COL_PERMANENT_WARD_NAME));
        u.setPhoneVerified(getIntAsBool(c, DatabaseHelper.COL_PHONE_VERIFIED));
        u.setVerified(getIntAsBool(c, DatabaseHelper.COL_IS_VERIFIED));
        u.setCreatedAt(getLongOrDefault(c, DatabaseHelper.COL_CREATED_AT, 0L));
        u.setUpdatedAt(getLongOrDefault(c, DatabaseHelper.COL_UPDATED_AT, 0L));
        return u;
    }

    private String getStringOrNull(Cursor c, String columnName) {
        int idx = c.getColumnIndex(columnName);
        return (idx != -1 && !c.isNull(idx)) ? c.getString(idx) : null;
    }

    private boolean getIntAsBool(Cursor c, String columnName) {
        int idx = c.getColumnIndex(columnName);
        return idx != -1 && c.getInt(idx) == 1;
    }

    private int getIntOrDefault(Cursor c, String columnName, int def) {
        int idx = c.getColumnIndex(columnName);
        return (idx != -1 && !c.isNull(idx)) ? c.getInt(idx) : def;
    }

    private long getLongOrDefault(Cursor c, String columnName, long def) {
        int idx = c.getColumnIndex(columnName);
        return (idx != -1 && !c.isNull(idx)) ? c.getLong(idx) : def;
    }

    private static String listToJson(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        org.json.JSONArray arr = new org.json.JSONArray();
        for (String s : list) arr.put(s);
        return arr.toString();
    }

    private static java.util.List<String> jsonToList(String json) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
            return list;
        } catch (Exception ignored) {}
        String[] parts = json.split(",");
        for (String s : parts) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }
}
