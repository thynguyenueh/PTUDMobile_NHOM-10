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

    public List<ChatRoom> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_CHAT_ROOMS, null, null, null,
                null, null, DatabaseHelper.COL_LAST_MESSAGE_AT + " DESC");
        List<ChatRoom> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    private ContentValues toValues(ChatRoom r) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_ROOM_ID, r.getRoomId());
        v.put("participants", listToJson(r.getParticipantIds()));   // ← ĐÃ SỬA
        v.put(DatabaseHelper.COL_LAST_MESSAGE, r.getLastMessage());
        v.put(DatabaseHelper.COL_LAST_MESSAGE_AT, r.getLastMessageTime());  // ← ĐÃ SỬA
        v.put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis());
        return v;
    }

    private ChatRoom cursorTo(Cursor c) {
        ChatRoom r = new ChatRoom();
        r.setRoomId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROOM_ID)));
        r.setParticipantIds(jsonToList(c.getString(c.getColumnIndexOrThrow("participants"))));  // ← ĐÃ SỬA
        r.setLastMessage(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE)));
        r.setLastMessageTime(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_MESSAGE_AT)));  // ← ĐÃ SỬA
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