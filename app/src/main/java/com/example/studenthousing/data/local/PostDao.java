package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.Constants;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class PostDao {
    private final DatabaseHelper dbHelper;

    public PostDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(AccommodationPost p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = toValues(p);
        db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<AccommodationPost> posts) {
        if (posts == null || posts.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (AccommodationPost p : posts) {
                ContentValues v = toValues(p);
                db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Replaces server-owned cache rows with a successfully fetched server snapshot, preserving pending local work. */
    public void replaceServerSnapshot(List<AccommodationPost> posts) {
        if (posts == null || posts.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.TBL_POSTS, DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " +
                    DatabaseHelper.COL_SYNC_STATUS + " NOT IN (?, ?, ?)",
                    new String[]{Constants.SYNC_PENDING_CREATE, Constants.SYNC_PENDING_UPDATE, Constants.SYNC_PENDING_DELETE});
            for (AccommodationPost p : posts) {
                AccommodationPost existing = getById(p.getPostId());
                if (existing != null && isPending(existing.getSyncStatus())) continue;
                p.setSyncStatus(Constants.SYNC_SYNCED);
                ContentValues v = toValues(p);
                db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private boolean isPending(String status) {
        return Constants.SYNC_PENDING_CREATE.equals(status) || Constants.SYNC_PENDING_UPDATE.equals(status) || Constants.SYNC_PENDING_DELETE.equals(status);
    }

    public List<AccommodationPost> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " + DatabaseHelper.COL_SYNC_STATUS + " != '" + Constants.SYNC_PENDING_DELETE + "'",
                null, null, null,
                DatabaseHelper.COL_CACHED_AT + " DESC");
        return cursorToList(c);
    }

    public AccommodationPost getById(String postId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_POST_ID + "=?",
                new String[]{postId}, null, null, null);
        if (c.moveToFirst()) {
            AccommodationPost p = cursorToPost(c);
            c.close();
            return p;
        }
        c.close();
        return null;
    }

    public void deleteById(String postId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_POSTS, DatabaseHelper.COL_POST_ID + "=?", new String[]{postId});
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_POSTS, null, null);
    }

    public int countByUserId(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TBL_POSTS +
                " WHERE " + DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public List<AccommodationPost> getByUserId(String userId) {
        List<AccommodationPost> list = new ArrayList<>();
        if (userId == null) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_USER_ID + "=? AND (" + DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " + DatabaseHelper.COL_SYNC_STATUS + " != '" + Constants.SYNC_PENDING_DELETE + "')",
                new String[]{userId}, null, null,
                DatabaseHelper.COL_CACHED_AT + " DESC");
        while (c.moveToNext()) {
            list.add(cursorToPost(c));
        }
        c.close();
        return list;
    }

    public boolean isSaved(String userId, String postId) {
        if (userId == null || postId == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TBL_SAVED_HOUSES, null,
                DatabaseHelper.COL_USER_ID + "=? AND " + DatabaseHelper.COL_POST_ID + "=?",
                new String[]{userId, postId}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    public void savePost(String userId, String postId) {
        if (userId == null || postId == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_USER_ID, userId);
        v.put(DatabaseHelper.COL_POST_ID, postId);
        v.put(DatabaseHelper.COL_SAVED_AT, System.currentTimeMillis());
        db.insertWithOnConflict(DatabaseHelper.TBL_SAVED_HOUSES, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void unsavePost(String userId, String postId) {
        if (userId == null || postId == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_SAVED_HOUSES,
                DatabaseHelper.COL_USER_ID + "=? AND " + DatabaseHelper.COL_POST_ID + "=?",
                new String[]{userId, postId});
    }

    public List<AccommodationPost> getSavedPosts(String userId) {
        List<AccommodationPost> list = new ArrayList<>();
        if (userId == null) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT p.* FROM " + DatabaseHelper.TBL_POSTS + " p " +
                "INNER JOIN " + DatabaseHelper.TBL_SAVED_HOUSES + " s ON p." + DatabaseHelper.COL_POST_ID + " = s." + DatabaseHelper.COL_POST_ID + " " +
                "WHERE s." + DatabaseHelper.COL_USER_ID + " = ? " +
                "ORDER BY s." + DatabaseHelper.COL_SAVED_AT + " DESC";
        Cursor c = db.rawQuery(sql, new String[]{userId});
        while (c.moveToNext()) {
            list.add(cursorToPost(c));
        }
        c.close();
        return list;
    }

    private ContentValues toValues(AccommodationPost p) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_POST_ID, p.getPostId());
        v.put(DatabaseHelper.COL_USER_ID, p.getUserId());
        v.put(DatabaseHelper.COL_FAMILY_NAME, p.getFamilyName());
        v.put(DatabaseHelper.COL_TITLE, p.getTitle());
        v.put(DatabaseHelper.COL_ADDRESS, p.getAddress());
        v.put(DatabaseHelper.COL_ADDRESS_FINGERPRINT, p.getAddressFingerprint());
        v.put(DatabaseHelper.COL_ADDRESS_VERIFICATION_STATUS, p.getAddressVerificationStatus());
        v.put(DatabaseHelper.COL_ROOM_NUMBER, p.getRoomNumber());
        v.put(DatabaseHelper.COL_CITY_CODE, p.getCityCode());
        v.put(DatabaseHelper.COL_WARD_CODE, p.getWardCode());
        v.put(DatabaseHelper.COL_CITY_NAME, p.getCityName());
        v.put(DatabaseHelper.COL_WARD_NAME, p.getWardName());
        v.put(DatabaseHelper.COL_LAT, p.getLatitude());
        v.put(DatabaseHelper.COL_LNG, p.getLongitude());
        v.put(DatabaseHelper.COL_DISTANCE_KM, p.getDistanceKm());
        v.put(DatabaseHelper.COL_DISTANCE_SOURCE, p.getDistanceSource());
        v.put(DatabaseHelper.COL_IS_DISTANCE_ESTIMATE, p.isDistanceEstimate() ? 1 : 0);
        v.put(DatabaseHelper.COL_LISTING_TYPE, p.getListingType());
        v.put(DatabaseHelper.COL_HOUSE_TYPE, p.getHouseType());
        v.put(DatabaseHelper.COL_PRICE, p.getPrice());
        v.put(DatabaseHelper.COL_AREA, p.getArea());
        v.put(DatabaseHelper.COL_DESCRIPTION, p.getDescription());
        v.put(DatabaseHelper.COL_IMAGES, listToJson(p.getImages()));
        v.put(DatabaseHelper.COL_UTILITIES, listToJson(p.getUtilities()));
        v.put(DatabaseHelper.COL_IS_POLICE_VERIFIED, p.isPoliceVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_VERIFICATION_BADGE, p.getVerificationBadge());
        v.put(DatabaseHelper.COL_STATUS, p.getStatus());
        v.put(DatabaseHelper.COL_CREATED_AT, p.getCreatedAt());
        v.put(DatabaseHelper.COL_UPDATED_AT, p.getUpdatedAt() > 0 ? p.getUpdatedAt() : System.currentTimeMillis());
        v.put(DatabaseHelper.COL_SYNC_STATUS, p.getSyncStatus() != null ? p.getSyncStatus() : Constants.SYNC_SYNCED);
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private List<AccommodationPost> cursorToList(Cursor c) {
        List<AccommodationPost> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(cursorToPost(c));
        }
        c.close();
        return list;
    }

    private AccommodationPost cursorToPost(Cursor c) {
        AccommodationPost p = new AccommodationPost();
        p.setPostId(getStringOrNull(c, DatabaseHelper.COL_POST_ID));
        p.setUserId(getStringOrNull(c, DatabaseHelper.COL_USER_ID));
        p.setFamilyName(getStringOrNull(c, DatabaseHelper.COL_FAMILY_NAME));
        p.setTitle(getStringOrNull(c, DatabaseHelper.COL_TITLE));
        p.setAddress(getStringOrNull(c, DatabaseHelper.COL_ADDRESS));
        p.setAddressFingerprint(getStringOrNull(c, DatabaseHelper.COL_ADDRESS_FINGERPRINT));
        p.setAddressVerificationStatus(getStringOrNull(c, DatabaseHelper.COL_ADDRESS_VERIFICATION_STATUS));
        p.setRoomNumber(getStringOrNull(c, DatabaseHelper.COL_ROOM_NUMBER));
        p.setCityCode(getIntOrDefault(c, DatabaseHelper.COL_CITY_CODE, 0));
        p.setWardCode(getIntOrDefault(c, DatabaseHelper.COL_WARD_CODE, 0));
        p.setCityName(getStringOrNull(c, DatabaseHelper.COL_CITY_NAME));
        p.setWardName(getStringOrNull(c, DatabaseHelper.COL_WARD_NAME));
        p.setLatitude(getDoubleOrDefault(c, DatabaseHelper.COL_LAT, 0.0));
        p.setLongitude(getDoubleOrDefault(c, DatabaseHelper.COL_LNG, 0.0));
        p.setDistanceKm(getDoubleOrDefault(c, DatabaseHelper.COL_DISTANCE_KM, 0.0));
        p.setDistanceSource(getStringOrNull(c, DatabaseHelper.COL_DISTANCE_SOURCE));
        p.setDistanceEstimate(getIntAsBool(c, DatabaseHelper.COL_IS_DISTANCE_ESTIMATE));
        p.setListingType(getStringOrNull(c, DatabaseHelper.COL_LISTING_TYPE));
        p.setHouseType(getStringOrNull(c, DatabaseHelper.COL_HOUSE_TYPE));
        p.setPrice(getDoubleOrDefault(c, DatabaseHelper.COL_PRICE, 0.0));
        p.setArea(getDoubleOrDefault(c, DatabaseHelper.COL_AREA, 0.0));
        p.setDescription(getStringOrNull(c, DatabaseHelper.COL_DESCRIPTION));
        p.setImages(jsonToList(getStringOrNull(c, DatabaseHelper.COL_IMAGES)));
        p.setUtilities(jsonToList(getStringOrNull(c, DatabaseHelper.COL_UTILITIES)));
        p.setPoliceVerified(getIntAsBool(c, DatabaseHelper.COL_IS_POLICE_VERIFIED));
        p.setVerificationBadge(getStringOrNull(c, DatabaseHelper.COL_VERIFICATION_BADGE));
        p.setStatus(getStringOrNull(c, DatabaseHelper.COL_STATUS));
        p.setCreatedAt(getLongOrDefault(c, DatabaseHelper.COL_CREATED_AT, 0L));
        p.setUpdatedAt(getLongOrDefault(c, DatabaseHelper.COL_UPDATED_AT, 0L));
        p.setSyncStatus(getStringOrNull(c, DatabaseHelper.COL_SYNC_STATUS));
        return p;
    }

    private String getStringOrNull(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getString(idx) : null;
    }

    private boolean getIntAsBool(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx != -1 && c.getInt(idx) == 1;
    }

    private int getIntOrDefault(Cursor c, String col, int def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getInt(idx) : def;
    }

    private double getDoubleOrDefault(Cursor c, String col, double def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getDouble(idx) : def;
    }

    private long getLongOrDefault(Cursor c, String col, long def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getLong(idx) : def;
    }

    private static String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        JSONArray arr = new JSONArray();
        for (String s : list) arr.put(s);
        return arr.toString();
    }

    private static List<String> jsonToList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
            return list;
        } catch (Exception ignored) {}
        return list;
    }
}
