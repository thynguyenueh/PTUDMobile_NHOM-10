package com.example.studenthousing.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "studenthousing.db";
    private static final int DB_VERSION = 2;

    // Table: posts_cache
    public static final String TBL_POSTS = "posts_cache";
    public static final String COL_POST_ID = "post_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_FAMILY_NAME = "family_name";
    public static final String COL_TITLE = "title";
    public static final String COL_ADDRESS = "address";
    public static final String COL_CITY_CODE = "city_code";
    public static final String COL_DISTRICT_CODE = "district_code";
    public static final String COL_WARD_CODE = "ward_code";
    public static final String COL_CITY_NAME = "city_name";
    public static final String COL_DISTRICT_NAME = "district_name";
    public static final String COL_WARD_NAME = "ward_name";
    public static final String COL_PRICE = "price";
    public static final String COL_AREA = "area";
    public static final String COL_HOUSE_TYPE = "house_type";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_IMAGES = "images";
    public static final String COL_UTILITIES = "utilities";
    public static final String COL_IS_POLICE_VERIFIED = "is_police_verified";
    public static final String COL_STATUS = "status";
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    public static final String COL_DISTANCE_KM = "distance_km";
    public static final String COL_CACHED_AT = "cached_at";
    public static final String COL_SYNC_STATUS = "sync_status";

    // Table: messages_cache
    public static final String TBL_MESSAGES = "messages_cache";
    public static final String COL_MSG_ID = "message_id";
    public static final String COL_ROOM_ID = "room_id";
    public static final String COL_SENDER_ID = "sender_id";
    public static final String COL_CONTENT = "content";
    public static final String COL_MSG_TYPE = "type";
    public static final String COL_IMAGE_URL = "image_url";
    public static final String COL_SENT_AT = "sent_at";
    public static final String COL_IS_READ = "is_read";

    // Table: exchange_requests_cache
    public static final String TBL_EXCHANGE = "exchange_requests_cache";
    public static final String COL_REQ_ID = "request_id";
    public static final String COL_SENDER_ID2 = "sender_id";
    public static final String COL_RECEIVER_ID = "receiver_id";
    public static final String COL_SENDER_POST_ID = "sender_post_id";
    public static final String COL_RECEIVER_POST_ID = "receiver_post_id";
    public static final String COL_CHAT_ROOM_ID = "chat_room_id";
    public static final String COL_EXCHANGE_PERIOD = "exchange_period";
    public static final String COL_MESSAGE = "message";
    public static final String COL_AGREEMENT_TEXT = "agreement_text";
    public static final String COL_REQ_STATUS = "status";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_RESPONDED_AT = "responded_at";
    public static final String COL_COMPLETED_AT = "completed_at";

    // Table: reviews_cache
    public static final String TBL_REVIEWS = "reviews_cache";
    public static final String COL_REVIEW_ID = "review_id";
    public static final String COL_REQUEST_ID = "request_id";
    public static final String COL_REVIEWER_ID = "reviewer_id";
    public static final String COL_REVIEWEE_ID = "reviewee_id";
    public static final String COL_RATING = "rating";
    public static final String COL_COMMENT = "comment";

    // Table: chat_rooms_cache
    public static final String TBL_CHAT_ROOMS = "chat_rooms_cache";
    public static final String COL_LAST_MESSAGE = "last_message";
    public static final String COL_LAST_MESSAGE_AT = "last_message_at";

    // Table: users_cache
    public static final String TBL_USERS = "users_cache";
    public static final String COL_PHONE = "phone";
    public static final String COL_EMAIL = "email";
    public static final String COL_AVATAR_URL = "avatar_url";
    public static final String COL_RESIDENCE_VERIFIED = "residence_verified";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. posts_cache
        db.execSQL("CREATE TABLE " + TBL_POSTS + " (" +
                COL_POST_ID + " TEXT PRIMARY KEY, " +
                COL_USER_ID + " TEXT, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_TITLE + " TEXT, " +
                COL_ADDRESS + " TEXT, " +
                COL_CITY_CODE + " INTEGER, " +
                COL_DISTRICT_CODE + " INTEGER, " +
                COL_WARD_CODE + " INTEGER, " +
                COL_CITY_NAME + " TEXT, " +
                COL_DISTRICT_NAME + " TEXT, " +
                COL_WARD_NAME + " TEXT, " +
                COL_PRICE + " REAL, " +
                COL_AREA + " REAL, " +
                COL_HOUSE_TYPE + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_IMAGES + " TEXT, " +
                COL_UTILITIES + " TEXT, " +
                COL_IS_POLICE_VERIFIED + " INTEGER, " +
                COL_STATUS + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LNG + " REAL, " +
                COL_DISTANCE_KM + " REAL, " +
                COL_CACHED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                ")");

        // 2. messages_cache
        db.execSQL("CREATE TABLE " + TBL_MESSAGES + " (" +
                COL_MSG_ID + " TEXT PRIMARY KEY, " +
                COL_ROOM_ID + " TEXT, " +
                COL_SENDER_ID + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_MSG_TYPE + " TEXT, " +
                COL_IMAGE_URL + " TEXT, " +
                COL_SENT_AT + " INTEGER, " +
                COL_IS_READ + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                ")");

        // 3. exchange_requests_cache
        db.execSQL("CREATE TABLE " + TBL_EXCHANGE + " (" +
                COL_REQ_ID + " TEXT PRIMARY KEY, " +
                COL_SENDER_ID2 + " TEXT, " +
                COL_RECEIVER_ID + " TEXT, " +
                COL_SENDER_POST_ID + " TEXT, " +
                COL_RECEIVER_POST_ID + " TEXT, " +
                COL_CHAT_ROOM_ID + " TEXT, " +
                COL_EXCHANGE_PERIOD + " TEXT, " +
                COL_MESSAGE + " TEXT, " +
                COL_AGREEMENT_TEXT + " TEXT, " +
                COL_REQ_STATUS + " TEXT, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_RESPONDED_AT + " INTEGER, " +
                COL_COMPLETED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                ")");

        // 4. reviews_cache
        db.execSQL("CREATE TABLE " + TBL_REVIEWS + " (" +
                COL_REVIEW_ID + " TEXT PRIMARY KEY, " +
                COL_REQUEST_ID + " TEXT, " +
                COL_REVIEWER_ID + " TEXT, " +
                COL_REVIEWEE_ID + " TEXT, " +
                COL_RATING + " INTEGER, " +
                COL_COMMENT + " TEXT, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                ")");

        // 5. chat_rooms_cache
        db.execSQL("CREATE TABLE " + TBL_CHAT_ROOMS + " (" +
                COL_ROOM_ID + " TEXT PRIMARY KEY, " +
                "participants TEXT, " +
                COL_LAST_MESSAGE + " TEXT, " +
                COL_LAST_MESSAGE_AT + " INTEGER, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 6. users_cache
        db.execSQL("CREATE TABLE " + TBL_USERS + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_PHONE + " TEXT, " +
                COL_EMAIL + " TEXT, " +
                COL_AVATAR_URL + " TEXT, " +
                COL_RESIDENCE_VERIFIED + " INTEGER, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // Index
        db.execSQL("CREATE INDEX idx_posts_city ON " + TBL_POSTS + "(" + COL_CITY_CODE + ")");
        db.execSQL("CREATE INDEX idx_msgs_room ON " + TBL_MESSAGES + "(" + COL_ROOM_ID + ")");
        db.execSQL("CREATE INDEX idx_exchange_sender ON " + TBL_EXCHANGE + "(" + COL_SENDER_ID2 + ")");
        db.execSQL("CREATE INDEX idx_exchange_receiver ON " + TBL_EXCHANGE + "(" + COL_RECEIVER_ID + ")");
        db.execSQL("CREATE INDEX idx_review_reviewee ON " + TBL_REVIEWS + "(" + COL_REVIEWEE_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_EXCHANGE + " (" +
                    COL_REQ_ID + " TEXT PRIMARY KEY, " +
                    COL_SENDER_ID2 + " TEXT, " +
                    COL_RECEIVER_ID + " TEXT, " +
                    COL_SENDER_POST_ID + " TEXT, " +
                    COL_RECEIVER_POST_ID + " TEXT, " +
                    COL_CHAT_ROOM_ID + " TEXT, " +
                    COL_EXCHANGE_PERIOD + " TEXT, " +
                    COL_MESSAGE + " TEXT, " +
                    COL_AGREEMENT_TEXT + " TEXT, " +
                    COL_REQ_STATUS + " TEXT, " +
                    COL_CREATED_AT + " INTEGER, " +
                    COL_RESPONDED_AT + " INTEGER, " +
                    COL_COMPLETED_AT + " INTEGER, " +
                    COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                    ")");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_REVIEWS + " (" +
                    COL_REVIEW_ID + " TEXT PRIMARY KEY, " +
                    COL_REQUEST_ID + " TEXT, " +
                    COL_REVIEWER_ID + " TEXT, " +
                    COL_REVIEWEE_ID + " TEXT, " +
                    COL_RATING + " INTEGER, " +
                    COL_COMMENT + " TEXT, " +
                    COL_CREATED_AT + " INTEGER, " +
                    COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED'" +
                    ")");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_CHAT_ROOMS + " (" +
                    COL_ROOM_ID + " TEXT PRIMARY KEY, " +
                    "participants TEXT, " +
                    COL_LAST_MESSAGE + " TEXT, " +
                    COL_LAST_MESSAGE_AT + " INTEGER, " +
                    COL_CACHED_AT + " INTEGER" +
                    ")");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_USERS + " (" +
                    COL_USER_ID + " TEXT PRIMARY KEY, " +
                    COL_FAMILY_NAME + " TEXT, " +
                    COL_PHONE + " TEXT, " +
                    COL_EMAIL + " TEXT, " +
                    COL_AVATAR_URL + " TEXT, " +
                    COL_RESIDENCE_VERIFIED + " INTEGER, " +
                    COL_CACHED_AT + " INTEGER" +
                    ")");

            // Index
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_exchange_sender ON " + TBL_EXCHANGE + "(" + COL_SENDER_ID2 + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_exchange_receiver ON " + TBL_EXCHANGE + "(" + COL_RECEIVER_ID + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_review_reviewee ON " + TBL_REVIEWS + "(" + COL_REVIEWEE_ID + ")");
        }
    }
}
