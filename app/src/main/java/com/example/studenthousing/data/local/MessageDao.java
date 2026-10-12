package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studenthousing.data.model.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    private final DatabaseHelper dbHelper;

    public MessageDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(Message msg) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = toValues(msg);
        db.insertWithOnConflict(DatabaseHelper.TBL_MESSAGES, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<Message> messages) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Message m : messages) {
                ContentValues v = toValues(m);
                db.insertWithOnConflict(DatabaseHelper.TBL_MESSAGES, null, v,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Message> getByRoomId(String roomId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_MESSAGES, null,
                DatabaseHelper.COL_ROOM_ID + "=?",
                new String[]{roomId}, null, null,
                DatabaseHelper.COL_SENT_AT + " ASC");
        List<Message> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(cursorToMessage(c));
        }
        c.close();
        return list;
    }

    private ContentValues toValues(Message m) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_MSG_ID, m.getMessageId());
        v.put(DatabaseHelper.COL_ROOM_ID, m.getRoomId());
        v.put(DatabaseHelper.COL_SENDER_ID, m.getSenderId());
        v.put(DatabaseHelper.COL_CONTENT, m.getContent());
        v.put(DatabaseHelper.COL_MSG_TYPE, m.getType());
        v.put(DatabaseHelper.COL_IMAGE_URL, m.getImageUrl());
        v.put(DatabaseHelper.COL_SENT_AT, m.getSentAt());
        v.put(DatabaseHelper.COL_IS_READ, m.isRead() ? 1 : 0);
        v.put(DatabaseHelper.COL_SYNC_STATUS, "SYNCED");
        return v;
    }

    private Message cursorToMessage(Cursor c) {
        Message m = new Message();
        m.setMessageId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_ID)));
        m.setRoomId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROOM_ID)));
        m.setSenderId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SENDER_ID)));
        m.setContent(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT)));
        m.setType(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_TYPE)));
        m.setImageUrl(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_IMAGE_URL)));
        m.setSentAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_SENT_AT)));
        m.setRead(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_IS_READ)) == 1);
        return m;
    }
}
