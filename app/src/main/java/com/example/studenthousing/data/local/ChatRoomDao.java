package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.ChatRoom;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomDao {
    private final DatabaseHelper dbHelper;

    public ChatRoomDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(ChatRoom r) {
        if (r == null || r.getRoomId() == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.insertWithOnConflict(DatabaseHelper.TBL_CHAT_ROOMS, null, toValues(r),
                    SQLiteDatabase.CONFLICT_REPLACE);

            if (r.getParticipantIds() != null) {
                db.delete(DatabaseHelper.TBL_CHAT_ROOM_PARTICIPANTS, "room_id=?", new String[]{r.getRoomId()});
                for (String userId : r.getParticipantIds()) {
                    if (userId == null || userId.trim().isEmpty()) continue;
                    ContentValues pv = new ContentValues();
                    pv.put("room_id", r.getRoomId());
                    pv.put("user_id", userId.trim());
                    db.insertWithOnConflict(DatabaseHelper.TBL_CHAT_ROOM_PARTICIPANTS, null, pv,
                            SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void upsertAll(List<ChatRoom> list) {
        if (list == null || list.isEmpty()) return;
        for (ChatRoom r : list) {
            upsert(r);
        }
    }

    public ChatRoom getById(String roomId) {
        if (roomId == null || roomId.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TBL_CHAT_ROOMS, null,
                DatabaseHelper.COL_ROOM_ID + "=?", new String[]{roomId}, null, null, null)) {
            if (c.moveToFirst()) return cursorTo(c);
        }
        return null;
    }

    public List<ChatRoom> getByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) return new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT r.* FROM " + DatabaseHelper.TBL_CHAT_ROOMS + " r " +
                "INNER JOIN " + DatabaseHelper.TBL_CHAT_ROOM_PARTICIPANTS + " p ON r." + DatabaseHelper.COL_ROOM_ID + " = p.room_id " +
                "WHERE p.user_id = ? " +
                "ORDER BY r." + DatabaseHelper.COL_LAST_MESSAGE_AT + " DESC";
        
        List<ChatRoom> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, new String[]{userId.trim()})) {
            while (c.moveToNext()) {
                list.add(cursorTo(c));
            }
        }
        return list;
    }

    public List<ChatRoom> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_CHAT_ROOMS, null,
                null, null, null, null, DatabaseHelper.COL_LAST_MESSAGE_AT + " DESC");
        List<ChatRoom> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                list.add(cursorTo(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private ContentValues toValues(ChatRoom r) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_ROOM_ID, r.getRoomId());
        v.put("participants", participantsToFormattedString(r.getParticipantIds()));
        v.put(DatabaseHelper.COL_LAST_MESSAGE, r.getLastMessage());
        v.put(DatabaseHelper.COL_LAST_MESSAGE_AT, r.getLastMessageTime());
        v.put("post_id", r.getPostId());
        v.put("post_title", r.getPostTitle());
        v.put("last_sender_id", r.getLastSenderId());
        v.put("created_at", r.getCreatedAt());
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private ChatRoom cursorTo(Cursor c) {
        ChatRoom r = new ChatRoom();
        r.setRoomId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROOM_ID)));
        r.setParticipantIds(formattedStringToParticipants(c.getString(c.getColumnIndexOrThrow("participants"))));
        r.setLastMessage(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE)));
        r.setLastMessageTime(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE_AT)));
        r.setPostId(c.getString(c.getColumnIndexOrThrow("post_id")));
        r.setPostTitle(c.getString(c.getColumnIndexOrThrow("post_title")));
        r.setLastSenderId(c.getString(c.getColumnIndexOrThrow("last_sender_id")));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return r;
    }

    private String participantsToFormattedString(List<String> list) {
        if (list == null || list.isEmpty()) return ",";
        StringBuilder sb = new StringBuilder(",");
        for (String s : list) {
            if (s != null && !s.trim().isEmpty()) {
                sb.append(s.trim()).append(",");
            }
        }
        return sb.toString();
    }

    private List<String> formattedStringToParticipants(String str) {
        List<String> list = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) return list;
        String[] parts = str.split(",");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }
}
