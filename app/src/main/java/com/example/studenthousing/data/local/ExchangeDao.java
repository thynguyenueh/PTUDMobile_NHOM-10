package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.ExchangeRequest;

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
        v.put(DatabaseHelper.COL_REQ_STATUS, r.getStatus());
        v.put(DatabaseHelper.COL_CREATED_AT, r.getCreatedAt());
        v.put(DatabaseHelper.COL_RESPONDED_AT, r.getRespondedAt());
        v.put(DatabaseHelper.COL_COMPLETED_AT, r.getCompletedAt());
        v.put(DatabaseHelper.COL_SYNC_STATUS, "SYNCED");
        return v;
    }

    private ExchangeRequest cursorTo(Cursor c) {
        ExchangeRequest r = new ExchangeRequest();
        r.setRequestId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REQ_ID)));
        r.setSenderId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SENDER_ID2)));
        r.setReceiverId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_RECEIVER_ID)));
        r.setSenderPostId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SENDER_POST_ID)));
        r.setReceiverPostId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_RECEIVER_POST_ID)));
        r.setChatRoomId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHAT_ROOM_ID)));
        r.setExchangePeriod(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXCHANGE_PERIOD)));
        r.setMessage(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MESSAGE)));
        r.setAgreementText(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_AGREEMENT_TEXT)));
        r.setStatus(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REQ_STATUS)));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)));
        r.setRespondedAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_RESPONDED_AT)));
        r.setCompletedAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMPLETED_AT)));
        return r;
    }
}
