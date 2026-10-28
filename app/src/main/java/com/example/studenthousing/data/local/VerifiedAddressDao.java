package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.util.Constants;

public class VerifiedAddressDao {
    private final DatabaseHelper dbHelper;

    public VerifiedAddressDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(VerifiedAddress addr) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null, toValues(addr),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public VerifiedAddress findByFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null,
                DatabaseHelper.COL_FINGERPRINT + "=?",
                new String[]{fingerprint}, null, null, null);
        if (c.moveToFirst()) {
            VerifiedAddress a = cursorTo(c);
            c.close();
            return a;
        }
        c.close();
        return null;
    }

    public java.util.List<VerifiedAddress> getByOwner(String ownerUserId) {
        java.util.List<VerifiedAddress> list = new java.util.ArrayList<>();
        if (ownerUserId == null || ownerUserId.isEmpty()) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TBL_VERIFIED_ADDRESSES, null,
                DatabaseHelper.COL_OWNER_USER_ID + "=?",
                new String[]{ownerUserId}, null, null, null)) {
            while (c.moveToNext()) {
                list.add(cursorTo(c));
            }
        }
        return list;
    }

    public void delete(String addressId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TBL_VERIFIED_ADDRESSES,
                DatabaseHelper.COL_ADDRESS_ID + "=?",
                new String[]{addressId});
    }

    private ContentValues toValues(VerifiedAddress a) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_ADDRESS_ID, a.getAddressId());
        v.put(DatabaseHelper.COL_FINGERPRINT, a.getFingerprint());
        v.put(DatabaseHelper.COL_CITY_CODE, a.getCityCode() > 0 ? a.getCityCode() : Constants.TPHCM_CITY_CODE);
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
        a.setCityCode(getIntOrDefault(c, DatabaseHelper.COL_CITY_CODE, Constants.TPHCM_CITY_CODE));
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
