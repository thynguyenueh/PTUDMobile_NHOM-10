package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PostDao {
    private final DatabaseHelper dbHelper;

    public PostDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(Post p) {
        if (p == null || p.getPostId() == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (!p.getPostId().startsWith("temp_") && p.getUserId() != null) {
            try {
                db.delete(DatabaseHelper.TBL_POSTS,
                        DatabaseHelper.COL_POST_ID + " LIKE ? AND " + DatabaseHelper.COL_USER_ID + "=?",
                        new String[]{"temp_%", p.getUserId()});
            } catch (Exception ignored) {}
        }
        ContentValues v = toValues(p);
        db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Replaces server-owned cache rows with a successfully fetched server snapshot, preserving pending local work. */
    public void replaceServerSnapshot(List<Post> posts) {
        if (posts == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            java.util.Set<String> pendingIds = new java.util.HashSet<>();
            try (Cursor c = db.query(DatabaseHelper.TBL_POSTS, new String[]{DatabaseHelper.COL_POST_ID},
                    DatabaseHelper.COL_SYNC_STATUS + " IN (?, ?, ?, ?)",
                    new String[]{Constants.SYNC_PENDING_CREATE, Constants.SYNC_PENDING_UPDATE, Constants.SYNC_PENDING_DELETE, Constants.SYNC_SYNCING},
                    null, null, null)) {
                while (c.moveToNext()) {
                    pendingIds.add(c.getString(0));
                }
            }

            for (Post p : posts) {
                if (p.getPostId() != null && pendingIds.contains(p.getPostId())) continue;
                p.setSyncStatus(Constants.SYNC_SYNCED);
                ContentValues v = toValues(p);
                db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Post> getAll() {
        return getAll(0);
    }

    public List<Post> getAll(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                "(" + DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " + DatabaseHelper.COL_SYNC_STATUS + " != '" + Constants.SYNC_PENDING_DELETE + "') AND " +
                "(" + DatabaseHelper.COL_STATUS + " IS NULL OR " + DatabaseHelper.COL_STATUS + " != '" + Constants.POST_EXCHANGED + "')",
                null, null, null,
                DatabaseHelper.COL_CACHED_AT + " DESC",
                limit > 0 ? String.valueOf(limit) : null);
        return cursorToList(c);
    }

    public Post getById(String postId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_POST_ID + "=?",
                new String[]{postId}, null, null, null);
        if (c.moveToFirst()) {
            Post p = cursorToPost(c);
            c.close();
            return p;
        }
        c.close();
        return null;
    }

    public Post getByFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_ADDRESS_FINGERPRINT + "=?",
                new String[]{fingerprint}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToPost(c);
            }
        }
        return null;
    }

    public void deleteById(String postId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_POSTS, DatabaseHelper.COL_POST_ID + "=?", new String[]{postId});
    }

    public int countByUserId(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TBL_POSTS +
                " WHERE " + DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public List<Post> getByUserId(String userId) {
        return getByUserId(userId, 0);
    }

    public List<Post> getByUserId(String userId, int limit) {
        if (userId == null) return new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                DatabaseHelper.COL_USER_ID + "=? AND (" + DatabaseHelper.COL_SYNC_STATUS + " IS NULL OR " + DatabaseHelper.COL_SYNC_STATUS + " != '" + Constants.SYNC_PENDING_DELETE + "')",
                new String[]{userId}, null, null,
                DatabaseHelper.COL_CACHED_AT + " DESC",
                limit > 0 ? String.valueOf(limit) : null);
        return cursorToList(c);
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

    public List<Post> getSavedPosts(String userId) {
        List<Post> list = new ArrayList<>();
        if (userId == null) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT p.* FROM " + DatabaseHelper.TBL_POSTS + " p " +
                "INNER JOIN " + DatabaseHelper.TBL_SAVED_HOUSES + " s ON p." + DatabaseHelper.COL_POST_ID + " = s." + DatabaseHelper.COL_POST_ID + " " +
                "WHERE s." + DatabaseHelper.COL_USER_ID + " = ? " +
                "ORDER BY s." + DatabaseHelper.COL_SAVED_AT + " DESC";
        try (Cursor c = db.rawQuery(sql, new String[]{userId})) {
            while (c.moveToNext()) {
                list.add(cursorToPost(c));
            }
        }
        return list;
    }

    private ContentValues toValues(Post p) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_POST_ID, p.getPostId());
        v.put(DatabaseHelper.COL_USER_ID, p.getUserId());
        v.put(DatabaseHelper.COL_FAMILY_NAME, p.getFamilyName());
        v.put(DatabaseHelper.COL_TITLE, p.getTitle());
        v.put(DatabaseHelper.COL_ADDRESS, p.getAddress());
        v.put(DatabaseHelper.COL_ADDRESS_FINGERPRINT, p.getAddressFingerprint());
        v.put(DatabaseHelper.COL_ADDRESS_VERIFICATION_STATUS, p.getAddressVerificationStatus());
        v.put(DatabaseHelper.COL_CITY_CODE, p.getCityCode() > 0 ? p.getCityCode() : Constants.TPHCM_CITY_CODE);
        v.put(DatabaseHelper.COL_WARD_CODE, p.getWardCode());
        v.put(DatabaseHelper.COL_CITY_NAME, p.getCityName() != null ? p.getCityName() : Constants.TPHCM_CITY_NAME);
        v.put(DatabaseHelper.COL_WARD_NAME, p.getWardName());
        v.put(DatabaseHelper.COL_STREET_NAME, p.getStreetName() != null ? p.getStreetName() : (p.getAddress() != null ? sanitizeStreetName(p.getAddress()) : null));
        v.put(DatabaseHelper.COL_HOUSE_NUMBER, p.getHouseNumber());
        v.put(DatabaseHelper.COL_DESCRIPTION, p.getDescription());
        v.put(DatabaseHelper.COL_IMAGES, listToJson(p.getImages()));
        v.put(DatabaseHelper.COL_UTILITIES, listToJson(p.getUtilities()));
        v.put(DatabaseHelper.COL_NEARBY_JSON, nearbyToJson(p.getNearbyCampuses()));
        v.put(DatabaseHelper.COL_STATUS, p.getStatus());
        v.put(DatabaseHelper.COL_CREATED_AT, p.getCreatedAt());
        v.put(DatabaseHelper.COL_UPDATED_AT, p.getUpdatedAt() > 0 ? p.getUpdatedAt() : System.currentTimeMillis());
        v.put(DatabaseHelper.COL_SYNC_STATUS, p.getSyncStatus() != null ? p.getSyncStatus() : Constants.SYNC_SYNCED);
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private List<Post> cursorToList(Cursor c) {
        List<Post> list = new ArrayList<>();
        if (c == null) return list;
        try {
            int idxPostId = c.getColumnIndex(DatabaseHelper.COL_POST_ID);
            int idxUserId = c.getColumnIndex(DatabaseHelper.COL_USER_ID);
            int idxFamilyName = c.getColumnIndex(DatabaseHelper.COL_FAMILY_NAME);
            int idxTitle = c.getColumnIndex(DatabaseHelper.COL_TITLE);
            int idxAddress = c.getColumnIndex(DatabaseHelper.COL_ADDRESS);
            int idxFp = c.getColumnIndex(DatabaseHelper.COL_ADDRESS_FINGERPRINT);
            int idxVerStatus = c.getColumnIndex(DatabaseHelper.COL_ADDRESS_VERIFICATION_STATUS);
            int idxCityCode = c.getColumnIndex(DatabaseHelper.COL_CITY_CODE);
            int idxWardCode = c.getColumnIndex(DatabaseHelper.COL_WARD_CODE);
            int idxCityName = c.getColumnIndex(DatabaseHelper.COL_CITY_NAME);
            int idxWardName = c.getColumnIndex(DatabaseHelper.COL_WARD_NAME);
            int idxStreetName = c.getColumnIndex(DatabaseHelper.COL_STREET_NAME);
            int idxHouseNumber = c.getColumnIndex(DatabaseHelper.COL_HOUSE_NUMBER);
            int idxDesc = c.getColumnIndex(DatabaseHelper.COL_DESCRIPTION);
            int idxImages = c.getColumnIndex(DatabaseHelper.COL_IMAGES);
            int idxUtils = c.getColumnIndex(DatabaseHelper.COL_UTILITIES);
            int idxNearby = c.getColumnIndex(DatabaseHelper.COL_NEARBY_JSON);
            int idxStatus = c.getColumnIndex(DatabaseHelper.COL_STATUS);
            int idxCreatedAt = c.getColumnIndex(DatabaseHelper.COL_CREATED_AT);
            int idxUpdatedAt = c.getColumnIndex(DatabaseHelper.COL_UPDATED_AT);
            int idxSync = c.getColumnIndex(DatabaseHelper.COL_SYNC_STATUS);

            while (c.moveToNext()) {
                Post p = new Post();
                if (idxPostId != -1 && !c.isNull(idxPostId)) p.setPostId(c.getString(idxPostId));
                if (idxUserId != -1 && !c.isNull(idxUserId)) p.setUserId(c.getString(idxUserId));
                if (idxFamilyName != -1 && !c.isNull(idxFamilyName)) p.setFamilyName(c.getString(idxFamilyName));
                if (idxTitle != -1 && !c.isNull(idxTitle)) p.setTitle(c.getString(idxTitle));
                if (idxAddress != -1 && !c.isNull(idxAddress)) p.setAddress(c.getString(idxAddress));
                if (idxFp != -1 && !c.isNull(idxFp)) p.setAddressFingerprint(c.getString(idxFp));
                if (idxVerStatus != -1 && !c.isNull(idxVerStatus)) p.setAddressVerificationStatus(c.getString(idxVerStatus));
                if (idxCityCode != -1 && !c.isNull(idxCityCode)) p.setCityCode(c.getInt(idxCityCode));
                if (idxWardCode != -1 && !c.isNull(idxWardCode)) p.setWardCode(c.getInt(idxWardCode));
                if (idxCityName != -1 && !c.isNull(idxCityName)) p.setCityName(c.getString(idxCityName));
                if (idxWardName != -1 && !c.isNull(idxWardName)) p.setWardName(c.getString(idxWardName));
                if (idxStreetName != -1 && !c.isNull(idxStreetName)) p.setStreetName(c.getString(idxStreetName));
                if (idxHouseNumber != -1 && !c.isNull(idxHouseNumber)) p.setHouseNumber(c.getString(idxHouseNumber));
                if (idxDesc != -1 && !c.isNull(idxDesc)) p.setDescription(c.getString(idxDesc));
                if (idxImages != -1 && !c.isNull(idxImages)) p.setImages(jsonToList(c.getString(idxImages)));
                if (idxUtils != -1 && !c.isNull(idxUtils)) p.setUtilities(jsonToList(c.getString(idxUtils)));
                if (idxNearby != -1 && !c.isNull(idxNearby)) p.setNearbyCampuses(jsonToNearby(c.getString(idxNearby)));
                if (idxStatus != -1 && !c.isNull(idxStatus)) p.setStatus(c.getString(idxStatus));
                if (idxCreatedAt != -1 && !c.isNull(idxCreatedAt)) p.setCreatedAt(c.getLong(idxCreatedAt));
                if (idxUpdatedAt != -1 && !c.isNull(idxUpdatedAt)) p.setUpdatedAt(c.getLong(idxUpdatedAt));
                if (idxSync != -1 && !c.isNull(idxSync)) p.setSyncStatus(c.getString(idxSync));
                list.add(p);
            }
        } finally {
            c.close();
        }
        return list;
    }

    private Post cursorToPost(Cursor c) {
        if (c == null) return null;
        Post p = new Post();
        p.setPostId(getStringOrNull(c, DatabaseHelper.COL_POST_ID));
        p.setUserId(getStringOrNull(c, DatabaseHelper.COL_USER_ID));
        p.setFamilyName(getStringOrNull(c, DatabaseHelper.COL_FAMILY_NAME));
        p.setTitle(getStringOrNull(c, DatabaseHelper.COL_TITLE));
        p.setAddress(getStringOrNull(c, DatabaseHelper.COL_ADDRESS));
        p.setAddressFingerprint(getStringOrNull(c, DatabaseHelper.COL_ADDRESS_FINGERPRINT));
        p.setAddressVerificationStatus(getStringOrNull(c, DatabaseHelper.COL_ADDRESS_VERIFICATION_STATUS));
        p.setCityCode(getIntOrDefault(c, DatabaseHelper.COL_CITY_CODE, Constants.TPHCM_CITY_CODE));
        p.setWardCode(getIntOrDefault(c, DatabaseHelper.COL_WARD_CODE, 0));
        p.setCityName(getStringOrNull(c, DatabaseHelper.COL_CITY_NAME));
        p.setWardName(getStringOrNull(c, DatabaseHelper.COL_WARD_NAME));
        p.setStreetName(getStringOrNull(c, DatabaseHelper.COL_STREET_NAME));
        p.setHouseNumber(getStringOrNull(c, DatabaseHelper.COL_HOUSE_NUMBER));
        p.setDescription(getStringOrNull(c, DatabaseHelper.COL_DESCRIPTION));
        p.setImages(jsonToList(getStringOrNull(c, DatabaseHelper.COL_IMAGES)));
        p.setUtilities(jsonToList(getStringOrNull(c, DatabaseHelper.COL_UTILITIES)));
        p.setNearbyCampuses(jsonToNearby(getStringOrNull(c, DatabaseHelper.COL_NEARBY_JSON)));
        p.setStatus(getStringOrNull(c, DatabaseHelper.COL_STATUS));
        p.setCreatedAt(getLongOrDefault(c, DatabaseHelper.COL_CREATED_AT, 0L));
        p.setUpdatedAt(getLongOrDefault(c, DatabaseHelper.COL_UPDATED_AT, 0L));
        p.setSyncStatus(getStringOrNull(c, DatabaseHelper.COL_SYNC_STATUS));
        return p;
    }

    private String nearbyToJson(List<NearbyCampus> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            JSONArray arr = new JSONArray();
            for (NearbyCampus n : list) {
                JSONObject obj = new JSONObject();
                obj.put("campusId", n.getCampusId());
                obj.put("km", n.getKm());
                obj.put("estimate", n.isEstimate());
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private List<NearbyCampus> jsonToNearby(String json) {
        List<NearbyCampus> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                NearbyCampus n = new NearbyCampus();
                n.setCampusId(obj.optString("campusId"));
                n.setKm(obj.optDouble("km"));
                n.setEstimate(obj.optBoolean("estimate"));
                list.add(n);
            }
        } catch (Exception e) {}
        return list;
    }

    private String sanitizeStreetName(String address) {
        if (address == null || address.trim().isEmpty()) return null;
        String[] parts = address.split(",");
        if (parts.length == 0) return address.trim();
        return parts[0].trim();
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

    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            JSONArray arr = new JSONArray(list);
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> jsonToList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i);
                if (s != null && !s.isEmpty()) list.add(s);
            }
        } catch (Exception e) {
            // ignore
        }
        return list;
    }
}
