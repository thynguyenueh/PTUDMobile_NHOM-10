package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class ExchangeDao {
    private final DatabaseHelper dbHelper;

    public ExchangeDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(ExchangeRequest r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_EXCHANGE, null, toValues(r),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<ExchangeRequest> list) {
        if (list == null || list.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ExchangeRequest r : list) {
                db.insertWithOnConflict(DatabaseHelper.TBL_EXCHANGE, null, toValues(r),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ExchangeRequest getById(String requestId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_EXCHANGE, null,
                DatabaseHelper.COL_REQ_ID + "=?",
                new String[]{requestId}, null, null, null);
        if (c.moveToFirst()) {
            ExchangeRequest r = cursorTo(c);
            c.close();
            return r;
        }
        c.close();
        return null;
    }

    public List<ExchangeRequest> getBySender(String senderId) {
        return queryBy(DatabaseHelper.COL_SENDER_ID2, senderId);
    }

    public List<ExchangeRequest> getByReceiver(String receiverId) {
        return queryBy(DatabaseHelper.COL_RECEIVER_ID, receiverId);
    }

    public List<ExchangeRequest> getPendingRequests() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_EXCHANGE, null,
                DatabaseHelper.COL_SYNC_STATUS + "=?",
                new String[]{Constants.SYNC_PENDING_CREATE}, null, null,
                DatabaseHelper.COL_CREATED_AT + " ASC");
        List<ExchangeRequest> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    public int countCompletedByUserId(String userId) {
        if (userId == null) return 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TBL_EXCHANGE +
                " WHERE (" + DatabaseHelper.COL_SENDER_ID2 + " = ? OR " + DatabaseHelper.COL_RECEIVER_ID + " = ?) " +
                "AND " + DatabaseHelper.COL_REQ_STATUS + " IN ('" + Constants.REQ_ACCEPTED + "', '" + Constants.REQ_COMPLETED + "')",
                new String[]{userId, userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<ExchangeRequest> queryBy(String column, String value) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_EXCHANGE, null,
                column + "=?", new String[]{value},
                null, null, DatabaseHelper.COL_CREATED_AT + " DESC");
        List<ExchangeRequest> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    private ContentValues toValues(ExchangeRequest r) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_REQ_ID, r.getRequestId());
        v.put(DatabaseHelper.COL_SENDER_ID2, r.getSenderId());
        v.put(DatabaseHelper.COL_RECEIVER_ID, r.getReceiverId());
        v.put(DatabaseHelper.COL_SENDER_POST_ID, r.getSenderPostId());
        v.put(DatabaseHelper.COL_RECEIVER_POST_ID, r.getReceiverPostId());
        v.put(DatabaseHelper.COL_CHAT_ROOM_ID, r.getChatRoomId());
        v.put(DatabaseHelper.COL_EXCHANGE_PERIOD, r.getExchangePeriod());
        v.put(DatabaseHelper.COL_MESSAGE, r.getMessage());
        v.put(DatabaseHelper.COL_AGREEMENT_TEXT, r.getAgreementText());
        v.put(DatabaseHelper.COL_SENDER_SIGNED_AT, r.getSenderSignedAt());
        v.put(DatabaseHelper.COL_RECEIVER_SIGNED_AT, r.getReceiverSignedAt());
        v.put(DatabaseHelper.COL_REQ_STATUS, r.getStatus());
        v.put(DatabaseHelper.COL_CREATED_AT, r.getCreatedAt());
        v.put(DatabaseHelper.COL_RESPONDED_AT, r.getRespondedAt());
        v.put(DatabaseHelper.COL_COMPLETED_AT, r.getCompletedAt());
        v.put(DatabaseHelper.COL_UPDATED_AT, r.getUpdatedAt() > 0 ? r.getUpdatedAt() : System.currentTimeMillis());
        v.put(DatabaseHelper.COL_SYNC_STATUS, r.getSyncStatus() != null ? r.getSyncStatus() : Constants.SYNC_SYNCED);
        return v;
    }

    private ExchangeRequest cursorTo(Cursor c) {
        ExchangeRequest r = new ExchangeRequest();
        r.setRequestId(getStringOrNull(c, DatabaseHelper.COL_REQ_ID));
        r.setSenderId(getStringOrNull(c, DatabaseHelper.COL_SENDER_ID2));
        r.setReceiverId(getStringOrNull(c, DatabaseHelper.COL_RECEIVER_ID));
        r.setSenderPostId(getStringOrNull(c, DatabaseHelper.COL_SENDER_POST_ID));
        r.setReceiverPostId(getStringOrNull(c, DatabaseHelper.COL_RECEIVER_POST_ID));
        r.setChatRoomId(getStringOrNull(c, DatabaseHelper.COL_CHAT_ROOM_ID));
        r.setExchangePeriod(getStringOrNull(c, DatabaseHelper.COL_EXCHANGE_PERIOD));
        r.setMessage(getStringOrNull(c, DatabaseHelper.COL_MESSAGE));
        r.setAgreementText(getStringOrNull(c, DatabaseHelper.COL_AGREEMENT_TEXT));
        r.setSenderSignedAt(getLongOrDefault(c, DatabaseHelper.COL_SENDER_SIGNED_AT, 0L));
        r.setReceiverSignedAt(getLongOrDefault(c, DatabaseHelper.COL_RECEIVER_SIGNED_AT, 0L));
        r.setStatus(getStringOrNull(c, DatabaseHelper.COL_REQ_STATUS));
        r.setCreatedAt(getLongOrDefault(c, DatabaseHelper.COL_CREATED_AT, 0L));
        r.setRespondedAt(getLongOrDefault(c, DatabaseHelper.COL_RESPONDED_AT, 0L));
        r.setCompletedAt(getLongOrDefault(c, DatabaseHelper.COL_COMPLETED_AT, 0L));
        r.setUpdatedAt(getLongOrDefault(c, DatabaseHelper.COL_UPDATED_AT, 0L));
        r.setSyncStatus(getStringOrNull(c, DatabaseHelper.COL_SYNC_STATUS));
        return r;
    }

    private String getStringOrNull(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getString(idx) : null;
    }

    private long getLongOrDefault(Cursor c, String col, long def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getLong(idx) : def;
    }
}
