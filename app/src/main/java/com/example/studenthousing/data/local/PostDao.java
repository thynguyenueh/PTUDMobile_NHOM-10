package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studenthousing.data.model.AccommodationPost;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class PostDao {
    private final DatabaseHelper dbHelper;

    public PostDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    // Lưu 1 post (upsert)
    public void upsert(AccommodationPost p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = toValues(p);
        db.insertWithOnConflict(DatabaseHelper.TBL_POSTS, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Lưu nhiều post (bulk)
    public void upsertAll(List<AccommodationPost> posts) {
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

    public List<AccommodationPost> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_POSTS, null,
                null, null, null, null,
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

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_POSTS, null, null);
    }

    // Mapping
    private ContentValues toValues(AccommodationPost p) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_POST_ID, p.getPostId());
        v.put(DatabaseHelper.COL_USER_ID, p.getUserId());
        v.put(DatabaseHelper.COL_FAMILY_NAME, p.getFamilyName());
        v.put(DatabaseHelper.COL_TITLE, p.getTitle());
        v.put(DatabaseHelper.COL_ADDRESS, p.getAddress());
        v.put(DatabaseHelper.COL_CITY_CODE, p.getCityCode());
        v.put(DatabaseHelper.COL_DISTRICT_CODE, p.getDistrictCode());
        v.put(DatabaseHelper.COL_WARD_CODE, p.getWardCode());
        v.put(DatabaseHelper.COL_CITY_NAME, p.getCityName());
        v.put(DatabaseHelper.COL_DISTRICT_NAME, p.getDistrictName());
        v.put(DatabaseHelper.COL_WARD_NAME, p.getWardName());
        v.put(DatabaseHelper.COL_PRICE, p.getPrice());
        v.put(DatabaseHelper.COL_AREA, p.getArea());
        v.put(DatabaseHelper.COL_HOUSE_TYPE, p.getHouseType());
        v.put(DatabaseHelper.COL_DESCRIPTION, p.getDescription());
        v.put(DatabaseHelper.COL_IMAGES, listToJson(p.getImages()));
        v.put(DatabaseHelper.COL_UTILITIES, listToJson(p.getUtilities()));
        v.put(DatabaseHelper.COL_IS_POLICE_VERIFIED, p.isPoliceVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_STATUS, p.getStatus());
        v.put(DatabaseHelper.COL_LAT, p.getLatitude());
        v.put(DatabaseHelper.COL_LNG, p.getLongitude());
        v.put(DatabaseHelper.COL_DISTANCE_KM, p.getDistanceKm());
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        v.put(DatabaseHelper.COL_SYNC_STATUS, "SYNCED");
        return v;
    }

    private AccommodationPost cursorToPost(Cursor c) {
        AccommodationPost p = new AccommodationPost();
        p.setPostId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_POST_ID)));
        p.setUserId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        p.setFamilyName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_FAMILY_NAME)));
        p.setTitle(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE)));
        p.setAddress(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ADDRESS)));
        p.setCityCode(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_CITY_CODE)));
        p.setDistrictCode(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_DISTRICT_CODE)));
        p.setWardCode(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_WARD_CODE)));
        p.setCityName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CITY_NAME)));
        p.setDistrictName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DISTRICT_NAME)));
        p.setWardName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_WARD_NAME)));
        p.setPrice(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_PRICE)));
        p.setArea(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_AREA)));
        p.setHouseType(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSE_TYPE)));
        p.setDescription(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION)));
        p.setImages(jsonToList(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_IMAGES))));
        p.setUtilities(jsonToList(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_UTILITIES))));
        p.setPoliceVerified(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_IS_POLICE_VERIFIED)) == 1);
        p.setStatus(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS)));
        p.setLatitude(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAT)));
        p.setLongitude(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_LNG)));
        p.setDistanceKm(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_DISTANCE_KM)));
        return p;
    }

    private List<AccommodationPost> cursorToList(Cursor c) {
        List<AccommodationPost> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(cursorToPost(c));
        }
        c.close();
        return list;
    }

    private String listToJson(List<String> list) {
        if (list == null) return "[]";
        JSONArray arr = new JSONArray();
        for (String s : list) arr.put(s);
        return arr.toString();
    }

    private List<String> jsonToList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (Exception ignored) {}
        return list;
    }
}
