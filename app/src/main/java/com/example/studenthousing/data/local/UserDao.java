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

    private ContentValues toValues(User u) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_USER_ID, u.getUserId());
        v.put(DatabaseHelper.COL_FAMILY_NAME, u.getFamilyName());
        v.put(DatabaseHelper.COL_PHONE, u.getPhone());
        v.put(DatabaseHelper.COL_EMAIL, u.getEmail());
        v.put(DatabaseHelper.COL_AVATAR_URL, u.getAvatarUrl());
        v.put(DatabaseHelper.COL_RESIDENCE_VERIFIED, u.isResidenceVerified() ? 1 : 0);
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private User cursorTo(Cursor c) {
        User u = new User();
        u.setUserId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        u.setFamilyName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_FAMILY_NAME)));
        u.setPhone(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE)));
        u.setEmail(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EMAIL)));
        u.setAvatarUrl(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_AVATAR_URL)));
        u.setResidenceVerified(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_RESIDENCE_VERIFIED)) == 1);
        return u;
    }
}
