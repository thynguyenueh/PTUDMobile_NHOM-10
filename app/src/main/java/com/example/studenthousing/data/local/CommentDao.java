package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentDao {
    private final DatabaseHelper dbHelper;

    public CommentDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void insert(Comment comment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_COMMENTS, null, toValues(comment), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Comment> getByPost(String postId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Comment> list = new ArrayList<>();
        Cursor c = db.query(DatabaseHelper.TBL_COMMENTS, null,
                DatabaseHelper.COL_COMMENT_POST_ID + "=?",
                new String[]{postId}, null, null,
                DatabaseHelper.COL_COMMENT_CREATED_AT + " ASC");
        while (c.moveToNext()) {
            list.add(cursorTo(c));
        }
        c.close();
        return list;
    }

    private ContentValues toValues(Comment c) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_COMMENT_ID, c.getCommentId());
        v.put(DatabaseHelper.COL_COMMENT_POST_ID, c.getPostId());
        v.put(DatabaseHelper.COL_COMMENT_USER_ID, c.getUserId());
        v.put(DatabaseHelper.COL_COMMENT_USER_NAME, c.getUserName());
        v.put(DatabaseHelper.COL_COMMENT_USER_AVATAR, c.getUserAvatar());
        v.put(DatabaseHelper.COL_COMMENT_CONTENT, c.getContent());
        v.put(DatabaseHelper.COL_COMMENT_CREATED_AT, c.getCreatedAt());
        return v;
    }

    private Comment cursorTo(Cursor c) {
        Comment cm = new Comment();
        cm.setCommentId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_ID)));
        cm.setPostId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_POST_ID)));
        cm.setUserId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_USER_ID)));
        cm.setUserName(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_USER_NAME)));
        cm.setUserAvatar(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_USER_AVATAR)));
        cm.setContent(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_CONTENT)));
        cm.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT_CREATED_AT)));
        return cm;
    }
}