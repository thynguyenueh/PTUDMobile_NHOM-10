package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.ChatRoom;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomDao {
    private final DatabaseHelper dbHelper;

    public ChatRoomDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(ChatRoom r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_CHAT_ROOMS, null, toValues(r),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<ChatRoom> list) {
        if (list == null || list.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ChatRoom r : list) {
                db.insertWithOnConflict(DatabaseHelper.TBL_CHAT_ROOMS, null, toValues(r),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<ChatRoom> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_CHAT_ROOMS, null, null, null,
                null, null, DatabaseHelper.COL_LAST_MESSAGE_AT + " DESC");
        List<ChatRoom> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    public List<ChatRoom> getByUserId(String userId) {
        if (userId == null) return new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_CHAT_ROOMS, null,
                "participants LIKE ?",
                new String[]{"%" + userId + "%"},
                null, null, DatabaseHelper.COL_LAST_MESSAGE_AT + " DESC");
        List<ChatRoom> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    private ContentValues toValues(ChatRoom r) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_ROOM_ID, r.getRoomId());
        v.put("participants", listToJson(r.getParticipantIds()));
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
        r.setParticipantIds(jsonToList(c.getString(c.getColumnIndexOrThrow("participants"))));
        r.setLastMessage(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE)));
        r.setLastMessageTime(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE_AT)));
        r.setPostId(c.getString(c.getColumnIndexOrThrow("post_id")));
        r.setPostTitle(c.getString(c.getColumnIndexOrThrow("post_title")));
        r.setLastSenderId(c.getString(c.getColumnIndexOrThrow("last_sender_id")));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return r;
    }

    private String listToJson(List<String> list) {
        if (list == null) return "[]";
        JSONArray a = new JSONArray();
        for (String s : list) a.put(s);
        return a.toString();
    }

    private List<String> jsonToList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) list.add(a.getString(i));
        } catch (Exception ignored) {}
        return list;
    }
}
