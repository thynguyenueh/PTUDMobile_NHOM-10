package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class VerificationTicketDao {
    private final DatabaseHelper dbHelper;

    public VerificationTicketDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(VerificationTicket ticket) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_VERIFICATION_TICKETS, null, toValues(ticket),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public VerificationTicket getById(String ticketId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFICATION_TICKETS, null,
                DatabaseHelper.COL_TICKET_ID + "=?",
                new String[]{ticketId}, null, null, null);
        if (c.moveToFirst()) {
            VerificationTicket t = cursorTo(c);
            c.close();
            return t;
        }
        c.close();
        return null;
    }

    public List<VerificationTicket> getPendingByUser(String userId) {
        List<VerificationTicket> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFICATION_TICKETS, null,
                DatabaseHelper.COL_USER_ID + "=? AND " + DatabaseHelper.COL_STATUS + "=?",
                new String[]{userId, Constants.VERIF_PENDING}, null, null, null);
        while (c.moveToNext()) {
            list.add(cursorTo(c));
        }
        c.close();
        return list;
    }

    public List<VerificationTicket> getAllByUser(String userId) {
        List<VerificationTicket> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_VERIFICATION_TICKETS, null,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{userId}, null, null,
                DatabaseHelper.COL_SUBMITTED_AT + " DESC");
        while (c.moveToNext()) {
            list.add(cursorTo(c));
        }
        c.close();
        return list;
    }

    private ContentValues toValues(VerificationTicket t) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_TICKET_ID, t.getTicketId());
        v.put(DatabaseHelper.COL_USER_ID, t.getUserId());
        v.put(DatabaseHelper.COL_TICKET_TYPE, t.getTicketType());
        v.put(DatabaseHelper.COL_FILE_URL, t.getFileUrl());
        v.put(DatabaseHelper.COL_METADATA_JSON, t.getMetadataJson());
        v.put(DatabaseHelper.COL_STATUS, t.getStatus());
        v.put(DatabaseHelper.COL_SUBMITTED_AT, t.getSubmittedAt());
        v.put(DatabaseHelper.COL_REVIEWED_AT, t.getReviewedAt());
        v.put(DatabaseHelper.COL_SYNC_STATUS, t.getSyncStatus());
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private VerificationTicket cursorTo(Cursor c) {
        VerificationTicket t = new VerificationTicket();
        t.setTicketId(getStringOrNull(c, DatabaseHelper.COL_TICKET_ID));
        t.setUserId(getStringOrNull(c, DatabaseHelper.COL_USER_ID));
        t.setTicketType(getStringOrNull(c, DatabaseHelper.COL_TICKET_TYPE));
        t.setFileUrl(getStringOrNull(c, DatabaseHelper.COL_FILE_URL));
        t.setMetadataJson(getStringOrNull(c, DatabaseHelper.COL_METADATA_JSON));
        t.setStatus(getStringOrNull(c, DatabaseHelper.COL_STATUS));
        t.setSubmittedAt(getLongOrDefault(c, DatabaseHelper.COL_SUBMITTED_AT, 0L));
        t.setReviewedAt(getLongOrDefault(c, DatabaseHelper.COL_REVIEWED_AT, 0L));
        t.setSyncStatus(getStringOrNull(c, DatabaseHelper.COL_SYNC_STATUS));
        t.setCachedAt(getLongOrDefault(c, DatabaseHelper.COL_CACHED_AT, 0L));
        return t;
    }

    private String getStringOrNull(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getString(idx) : null;
    }

    private long getLongOrDefault(Cursor c, String col, long defVal) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getLong(idx) : defVal;
    }
}
