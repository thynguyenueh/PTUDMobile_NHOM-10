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

    public Message getById(String messageId) {
        if (messageId == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_MESSAGES, null,
                DatabaseHelper.COL_MSG_ID + "=?",
                new String[]{messageId}, null, null, null);
        if (c.moveToFirst()) {
            Message m = cursorToMessage(c);
            c.close();
            return m;
        }
        c.close();
        return null;
    }

    public void upsert(Message msg) {
        if (msg == null || msg.getMessageId() == null) return;
        Message existing = getById(msg.getMessageId());
        if (existing != null && existing.isRead()) {
            msg.setRead(true);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = toValues(msg);
        db.insertWithOnConflict(DatabaseHelper.TBL_MESSAGES, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Message m : messages) {
                if (m == null || m.getMessageId() == null) continue;
                Message existing = getById(m.getMessageId());
                if (existing != null && existing.isRead()) {
                    m.setRead(true);
                }
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
        v.put(DatabaseHelper.COL_SYNC_STATUS, m.getSyncStatus() != null ? m.getSyncStatus() : "SYNCED");
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
        int syncIdx = c.getColumnIndex(DatabaseHelper.COL_SYNC_STATUS);
        if (syncIdx != -1) {
            m.setSyncStatus(c.getString(syncIdx));
        } else {
            m.setSyncStatus("SYNCED");
        }
        return m;
    }

    public List<Message> getPendingMessages() {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_MESSAGES, null,
                DatabaseHelper.COL_SYNC_STATUS + "=?",
                new String[]{"PENDING_CREATE"}, null, null,
                DatabaseHelper.COL_SENT_AT + " ASC");
        while (c.moveToNext()) {
            list.add(cursorToMessage(c));
        }
        c.close();
        return list;
    }

    public int countUnreadByRoom(String roomId, String currentUserId) {
        if (roomId == null || currentUserId == null) return 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TBL_MESSAGES +
                " WHERE " + DatabaseHelper.COL_ROOM_ID + " = ? AND " +
                DatabaseHelper.COL_IS_READ + " = 0 AND " +
                DatabaseHelper.COL_SENDER_ID + " != ?";
        try (Cursor c = db.rawQuery(query, new String[]{roomId, currentUserId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void markAsRead(String roomId, String currentUserId) {
        if (roomId == null || currentUserId == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_IS_READ, 1);
        try {
            db.update(DatabaseHelper.TBL_MESSAGES, v,
                    DatabaseHelper.COL_ROOM_ID + "=? AND " + DatabaseHelper.COL_SENDER_ID + "!=?",
                    new String[]{roomId, currentUserId});
        } catch (Exception ignored) {}
    }
}
