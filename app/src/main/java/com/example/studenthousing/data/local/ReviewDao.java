package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewDao {
    private final DatabaseHelper dbHelper;

    public ReviewDao(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx);
    }

    public void upsert(Review r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TBL_REVIEWS, null, toValues(r),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void upsertAll(List<Review> list) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Review r : list) {
                db.insertWithOnConflict(DatabaseHelper.TBL_REVIEWS, null, toValues(r),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Review> getByReviewee(String revieweeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_REVIEWS, null,
                DatabaseHelper.COL_REVIEWEE_ID + "=?",
                new String[]{revieweeId},
                null, null, DatabaseHelper.COL_CREATED_AT + " DESC");
        List<Review> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    public List<Review> getByRequest(String requestId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_REVIEWS, null,
                DatabaseHelper.COL_REQUEST_ID + "=?",
                new String[]{requestId},
                null, null, null);
        List<Review> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    private ContentValues toValues(Review r) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_REVIEW_ID, r.getReviewId());
        v.put(DatabaseHelper.COL_REQUEST_ID, r.getRequestId());
        v.put(DatabaseHelper.COL_REVIEWER_ID, r.getReviewerId());
        v.put(DatabaseHelper.COL_REVIEWEE_ID, r.getRevieweeId());
        v.put(DatabaseHelper.COL_RATING, r.getRating());
        v.put(DatabaseHelper.COL_COMMENT, r.getComment());
        v.put(DatabaseHelper.COL_CREATED_AT, r.getCreatedAt());
        v.put(DatabaseHelper.COL_SYNC_STATUS, "SYNCED");
        return v;
    }

    private Review cursorTo(Cursor c) {
        Review r = new Review();
        r.setReviewId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_ID)));
        r.setRequestId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REQUEST_ID)));
        r.setReviewerId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEWER_ID)));
        r.setRevieweeId(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEWEE_ID)));
        r.setRating(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_RATING)));
        r.setComment(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENT)));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)));
        return r;
    }
}
