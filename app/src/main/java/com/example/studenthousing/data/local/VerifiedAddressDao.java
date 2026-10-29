package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.VerifiedAddress;

import java.util.ArrayList;
import java.util.List;

public class VerifiedAddressDao {
    private final DatabaseHelper dbHelper;

    public VerifiedAddressDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(VerifiedAddress address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null, toValues(address),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<VerifiedAddress> getByUserId(String userId) {
        List<VerifiedAddress> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null,
                DatabaseHelper.COL_OWNER_USER_ID + "=?",
                new String[]{userId}, null, null,
                DatabaseHelper.COL_VERIFIED_AT + " DESC");
        while (c.moveToNext()) {
            list.add(cursorTo(c));
        }
        c.close();
        return list;
    }

    public VerifiedAddress getByAddressAndUser(String userId, String fullAddress) {
        if (userId == null || fullAddress == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null,
                DatabaseHelper.COL_OWNER_USER_ID + "=? AND " + DatabaseHelper.COL_FULL_ADDRESS + "=?",
                new String[]{userId, fullAddress.trim()}, null, null, null);
        if (c.moveToFirst()) {
            VerifiedAddress a = cursorTo(c);
            c.close();
            return a;
        }
        c.close();
        return null;
    }

    private ContentValues toValues(VerifiedAddress a) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_ADDRESS_ID, a.getAddressId());
        v.put(DatabaseHelper.COL_FINGERPRINT, a.getFingerprint());
        v.put(DatabaseHelper.COL_CITY_CODE, a.getCityCode());
        v.put(DatabaseHelper.COL_WARD_CODE, a.getWardCode());
        v.put(DatabaseHelper.COL_STREET_NORMALIZED, a.getStreetNormalized());
        v.put(DatabaseHelper.COL_HOUSE_NUMBER_NORM, a.getHouseNumberNorm());
        v.put(DatabaseHelper.COL_FULL_ADDRESS, a.getFullAddress());
        v.put(DatabaseHelper.COL_OWNER_USER_ID, a.getOwnerUserId());
        v.put(DatabaseHelper.COL_VERIFICATION_TICKET_ID, a.getVerificationTicketId());
        v.put(DatabaseHelper.COL_VERIFIED_AT, a.getVerifiedAt());
        v.put(DatabaseHelper.COL_STATUS, a.getStatus());
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private VerifiedAddress cursorTo(Cursor c) {
        VerifiedAddress a = new VerifiedAddress();
        a.setAddressId(getStringOrNull(c, DatabaseHelper.COL_ADDRESS_ID));
        a.setFingerprint(getStringOrNull(c, DatabaseHelper.COL_FINGERPRINT));
        a.setCityCode(getIntOrDefault(c, DatabaseHelper.COL_CITY_CODE, 79));
        a.setWardCode(getIntOrDefault(c, DatabaseHelper.COL_WARD_CODE, 0));
        a.setStreetNormalized(getStringOrNull(c, DatabaseHelper.COL_STREET_NORMALIZED));
        a.setHouseNumberNorm(getStringOrNull(c, DatabaseHelper.COL_HOUSE_NUMBER_NORM));
        a.setFullAddress(getStringOrNull(c, DatabaseHelper.COL_FULL_ADDRESS));
        a.setOwnerUserId(getStringOrNull(c, DatabaseHelper.COL_OWNER_USER_ID));
        a.setVerificationTicketId(getStringOrNull(c, DatabaseHelper.COL_VERIFICATION_TICKET_ID));
        a.setVerifiedAt(getLongOrDefault(c, DatabaseHelper.COL_VERIFIED_AT, 0L));
        a.setStatus(getStringOrNull(c, DatabaseHelper.COL_STATUS));
        a.setCachedAt(getLongOrDefault(c, DatabaseHelper.COL_CACHED_AT, 0L));
        return a;
    }

    private String getStringOrNull(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getString(idx) : null;
    }

    private int getIntOrDefault(Cursor c, String col, int defVal) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getInt(idx) : defVal;
    }

    private long getLongOrDefault(Cursor c, String col, long defVal) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getLong(idx) : defVal;
    }
}
