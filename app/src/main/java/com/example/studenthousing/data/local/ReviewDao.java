package com.example.studenthousing.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.util.Constants;

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
        if (list == null || list.isEmpty()) return;
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

    public List<Review> getByReviewer(String reviewerId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_REVIEWS, null,
                DatabaseHelper.COL_REVIEWER_ID + "=?",
                new String[]{reviewerId},
                null, null, DatabaseHelper.COL_CREATED_AT + " DESC");
        List<Review> list = new ArrayList<>();
        while (c.moveToNext()) list.add(cursorTo(c));
        c.close();
        return list;
    }

    public List<Review> getPendingReviews() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TBL_REVIEWS, null,
                DatabaseHelper.COL_SYNC_STATUS + "=?",
                new String[]{Constants.SYNC_PENDING_CREATE}, null, null,
                DatabaseHelper.COL_CREATED_AT + " ASC");
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
        v.put(DatabaseHelper.COL_REVIEWER_NAME, r.getReviewerName());
        v.put(DatabaseHelper.COL_REVIEWEE_ID, r.getRevieweeId());
        v.put(DatabaseHelper.COL_REVIEWEE_NAME, r.getRevieweeName());
        v.put(DatabaseHelper.COL_RATING, r.getRating());
        v.put(DatabaseHelper.COL_COMMENT, r.getComment());
        v.put(DatabaseHelper.COL_IMAGE_URI, r.getImageUri());
        v.put(DatabaseHelper.COL_CREATED_AT, r.getCreatedAt());
        v.put(DatabaseHelper.COL_SYNC_STATUS, r.getSyncStatus() != null ? r.getSyncStatus() : Constants.SYNC_SYNCED);
        return v;
    }

    private Review cursorTo(Cursor c) {
        Review r = new Review();
        r.setReviewId(getStringOrNull(c, DatabaseHelper.COL_REVIEW_ID));
        r.setRequestId(getStringOrNull(c, DatabaseHelper.COL_REQUEST_ID));
        r.setReviewerId(getStringOrNull(c, DatabaseHelper.COL_REVIEWER_ID));
        r.setReviewerName(getStringOrNull(c, DatabaseHelper.COL_REVIEWER_NAME));
        r.setRevieweeId(getStringOrNull(c, DatabaseHelper.COL_REVIEWEE_ID));
        r.setRevieweeName(getStringOrNull(c, DatabaseHelper.COL_REVIEWEE_NAME));
        r.setRating(getDoubleOrDefault(c, DatabaseHelper.COL_RATING, 0.0));
        r.setComment(getStringOrNull(c, DatabaseHelper.COL_COMMENT));
        r.setImageUri(getStringOrNull(c, DatabaseHelper.COL_IMAGE_URI));
        r.setCreatedAt(getLongOrDefault(c, DatabaseHelper.COL_CREATED_AT, 0L));
        r.setSyncStatus(getStringOrNull(c, DatabaseHelper.COL_SYNC_STATUS));
        return r;
    }

    private String getStringOrNull(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getString(idx) : null;
    }

    private double getDoubleOrDefault(Cursor c, String col, double def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getDouble(idx) : def;
    }

    private long getLongOrDefault(Cursor c, String col, long def) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getLong(idx) : def;
    }
}
