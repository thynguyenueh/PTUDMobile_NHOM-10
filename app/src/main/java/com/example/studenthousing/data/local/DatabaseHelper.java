package com.example.studenthousing.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "studenthousing.db";
    private static final int DB_VERSION = 10;

    // Table: posts_cache
    public static final String TBL_POSTS = "posts_cache";
    public static final String TBL_SAVED_HOUSES = "saved_houses";
    public static final String COL_SAVED_AT = "saved_at";
    public static final String COL_POST_ID = "post_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_FAMILY_NAME = "family_name";
    public static final String COL_TITLE = "title";
    public static final String COL_ADDRESS = "address";
    public static final String COL_ADDRESS_FINGERPRINT = "address_fingerprint";
    public static final String COL_ADDRESS_VERIFICATION_STATUS = "address_verification_status";
    public static final String COL_ROOM_NUMBER = "room_number";
    public static final String COL_CITY_CODE = "city_code";
    public static final String COL_WARD_CODE = "ward_code";
    public static final String COL_CITY_NAME = "city_name";
    public static final String COL_WARD_NAME = "ward_name";
    public static final String COL_PRICE = "price";
    public static final String COL_AREA = "area";
    public static final String COL_LISTING_TYPE = "listing_type";
    public static final String COL_HOUSE_TYPE = "house_type";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_IMAGES = "images";
    public static final String COL_UTILITIES = "utilities";
    public static final String COL_IS_POLICE_VERIFIED = "is_police_verified";
    public static final String COL_VERIFICATION_BADGE = "verification_badge";
    public static final String COL_STATUS = "status";
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    public static final String COL_DISTANCE_KM = "distance_km";
    public static final String COL_DISTANCE_SOURCE = "distance_source";
    public static final String COL_IS_DISTANCE_ESTIMATE = "is_distance_estimate";
    public static final String COL_CACHED_AT = "cached_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_SYNC_STATUS = "sync_status";

    // Table: verified_addresses
    public static final String TBL_VERIFIED_ADDRESSES = "verified_addresses";
    public static final String COL_ADDRESS_ID = "address_id";
    public static final String COL_FINGERPRINT = "fingerprint";
    public static final String COL_STREET_NORMALIZED = "street_normalized";
    public static final String COL_HOUSE_NUMBER_NORM = "house_number_norm";
    public static final String COL_ROOM_NUMBER_NORM = "room_number_norm";
    public static final String COL_FULL_ADDRESS = "full_address";
    public static final String COL_OWNER_USER_ID = "owner_user_id";
    public static final String COL_VERIFICATION_TICKET_ID = "verification_ticket_id";
    public static final String COL_VERIFIED_AT = "verified_at";

    // Table: verification_tickets
    public static final String TBL_VERIFICATION_TICKETS = "verification_tickets";
    public static final String COL_TICKET_ID = "ticket_id";
    public static final String COL_TICKET_TYPE = "ticket_type";
    public static final String COL_FILE_URL = "file_url";
    public static final String COL_METADATA_JSON = "metadata_json";
    public static final String COL_SUBMITTED_AT = "submitted_at";
    public static final String COL_REVIEWED_AT = "reviewed_at";

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
    public static final String COL_SENDER_SIGNED_AT = "sender_signed_at";
    public static final String COL_RECEIVER_SIGNED_AT = "receiver_signed_at";
    public static final String COL_REQ_STATUS = "status";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_RESPONDED_AT = "responded_at";
    public static final String COL_COMPLETED_AT = "completed_at";

    // Table: reviews_cache
    public static final String TBL_REVIEWS = "reviews_cache";
    public static final String COL_REVIEW_ID = "review_id";
    public static final String COL_REQUEST_ID = "request_id";
    public static final String COL_REVIEWER_ID = "reviewer_id";
    public static final String COL_REVIEWER_NAME = "reviewer_name";
    public static final String COL_REVIEWEE_ID = "reviewee_id";
    public static final String COL_REVIEWEE_NAME = "reviewee_name";
    public static final String COL_RATING = "rating";
    public static final String COL_COMMENT = "comment";
    public static final String COL_IMAGE_URI = "image_uri";

    // Table: chat_rooms_cache
    public static final String TBL_CHAT_ROOMS = "chat_rooms_cache";
    public static final String COL_LAST_MESSAGE = "last_message";
    public static final String COL_LAST_MESSAGE_AT = "last_message_at";

    // Table: users_cache
    public static final String TBL_USERS = "users_cache";
    public static final String COL_PHONE = "phone";
    public static final String COL_EMAIL = "email";
    public static final String COL_AVATAR_URL = "avatar_url";
    public static final String COL_IDENTITY_TYPE = "identity_type";
    public static final String COL_IDENTITY_VERIFIED = "identity_verified";
    public static final String COL_IDENTITY_VERIFIED_AT = "identity_verified_at";
    public static final String COL_STUDENT_ID = "student_id";
    public static final String COL_UNIVERSITY_CODE = "university_code";
    public static final String COL_UNIVERSITY_NAME = "university_name";
    public static final String COL_CITIZEN_ID_HASH = "citizen_id_hash";
    public static final String COL_RESIDENCE_VERIFIED = "residence_verified";
    public static final String COL_RESIDENCE_VERIFIED_AT = "residence_verified_at";
    public static final String COL_FULLY_VERIFIED = "fully_verified";
    public static final String COL_PERMANENT_ADDRESS = "permanent_address";
    public static final String COL_PERMANENT_CITY_CODE = "permanent_city_code";
    public static final String COL_PERMANENT_WARD_CODE = "permanent_ward_code";
    public static final String COL_PERMANENT_CITY_NAME = "permanent_city_name";
    public static final String COL_PERMANENT_WARD_NAME = "permanent_ward_name";
    public static final String COL_AUTH_PROVIDERS = "auth_providers";
    public static final String COL_PHONE_VERIFIED = "phone_verified";
    public static final String COL_IS_VERIFIED = "is_verified";

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
        // 1. users_cache
        db.execSQL("CREATE TABLE " + TBL_USERS + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_PHONE + " TEXT, " +
                COL_EMAIL + " TEXT, " +
                "password TEXT, " +
                COL_AVATAR_URL + " TEXT, " +
                COL_IDENTITY_TYPE + " TEXT DEFAULT 'NONE', " +
                COL_IDENTITY_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_IDENTITY_VERIFIED_AT + " INTEGER DEFAULT 0, " +
                COL_STUDENT_ID + " TEXT, " +
                COL_UNIVERSITY_CODE + " TEXT, " +
                COL_UNIVERSITY_NAME + " TEXT, " +
                COL_CITIZEN_ID_HASH + " TEXT, " +
                COL_RESIDENCE_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_RESIDENCE_VERIFIED_AT + " INTEGER DEFAULT 0, " +
                COL_FULLY_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_PERMANENT_ADDRESS + " TEXT, " +
                COL_PERMANENT_CITY_CODE + " INTEGER DEFAULT 0, " +
                COL_PERMANENT_WARD_CODE + " INTEGER DEFAULT 0, " +
                COL_PERMANENT_CITY_NAME + " TEXT, " +
                COL_PERMANENT_WARD_NAME + " TEXT, " +
                COL_AUTH_PROVIDERS + " TEXT, " +
                COL_PHONE_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_IS_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 2. verified_addresses
        db.execSQL("CREATE TABLE " + TBL_VERIFIED_ADDRESSES + " (" +
                COL_ADDRESS_ID + " TEXT PRIMARY KEY, " +
                COL_FINGERPRINT + " TEXT UNIQUE, " +
                COL_CITY_CODE + " INTEGER, " +
                COL_WARD_CODE + " INTEGER, " +
                COL_STREET_NORMALIZED + " TEXT, " +
                COL_HOUSE_NUMBER_NORM + " TEXT, " +
                COL_ROOM_NUMBER_NORM + " TEXT, " +
                COL_FULL_ADDRESS + " TEXT, " +
                COL_OWNER_USER_ID + " TEXT, " +
                COL_VERIFICATION_TICKET_ID + " TEXT, " +
                COL_VERIFIED_AT + " INTEGER, " +
                COL_STATUS + " TEXT DEFAULT 'VERIFIED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 3. verification_tickets
        db.execSQL("CREATE TABLE " + TBL_VERIFICATION_TICKETS + " (" +
                COL_TICKET_ID + " TEXT PRIMARY KEY, " +
                COL_USER_ID + " TEXT, " +
                COL_TICKET_TYPE + " TEXT, " +
                COL_FILE_URL + " TEXT, " +
                COL_METADATA_JSON + " TEXT, " +
                COL_STATUS + " TEXT DEFAULT 'PENDING', " +
                COL_SUBMITTED_AT + " INTEGER, " +
                COL_REVIEWED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 4. posts_cache
        db.execSQL("CREATE TABLE " + TBL_POSTS + " (" +
                COL_POST_ID + " TEXT PRIMARY KEY, " +
                COL_USER_ID + " TEXT, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_TITLE + " TEXT, " +
                COL_ADDRESS + " TEXT, " +
                COL_ADDRESS_FINGERPRINT + " TEXT, " +
                COL_ADDRESS_VERIFICATION_STATUS + " TEXT DEFAULT 'UNVERIFIED', " +
                COL_ROOM_NUMBER + " TEXT, " +
                COL_CITY_CODE + " INTEGER, " +
                COL_WARD_CODE + " INTEGER, " +
                COL_CITY_NAME + " TEXT, " +
                COL_WARD_NAME + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LNG + " REAL, " +
                COL_DISTANCE_KM + " REAL, " +
                COL_DISTANCE_SOURCE + " TEXT, " +
                COL_IS_DISTANCE_ESTIMATE + " INTEGER DEFAULT 0, " +
                COL_LISTING_TYPE + " TEXT DEFAULT 'EXCHANGE', " +
                COL_HOUSE_TYPE + " TEXT, " +
                COL_PRICE + " REAL, " +
                COL_AREA + " REAL, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_IMAGES + " TEXT, " +
                COL_UTILITIES + " TEXT, " +
                COL_IS_POLICE_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_VERIFICATION_BADGE + " TEXT, " +
                COL_STATUS + " TEXT DEFAULT 'AVAILABLE', " +
                COL_CREATED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 5. chat_rooms_cache
        db.execSQL("CREATE TABLE " + TBL_CHAT_ROOMS + " (" +
                COL_ROOM_ID + " TEXT PRIMARY KEY, " +
                "participants TEXT, " +
                COL_LAST_MESSAGE + " TEXT, " +
                COL_LAST_MESSAGE_AT + " INTEGER, " +
                "post_id TEXT, " +
                "post_title TEXT, " +
                "last_sender_id TEXT, " +
                "created_at INTEGER, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 6. messages_cache
        db.execSQL("CREATE TABLE " + TBL_MESSAGES + " (" +
                COL_MSG_ID + " TEXT PRIMARY KEY, " +
                COL_ROOM_ID + " TEXT, " +
                COL_SENDER_ID + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_MSG_TYPE + " TEXT, " +
                COL_IMAGE_URL + " TEXT, " +
                COL_SENT_AT + " INTEGER, " +
                COL_IS_READ + " INTEGER DEFAULT 0, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 7. exchange_requests_cache
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
                COL_SENDER_SIGNED_AT + " INTEGER DEFAULT 0, " +
                COL_RECEIVER_SIGNED_AT + " INTEGER DEFAULT 0, " +
                COL_REQ_STATUS + " TEXT DEFAULT 'PENDING', " +
                COL_CREATED_AT + " INTEGER, " +
                COL_RESPONDED_AT + " INTEGER, " +
                COL_COMPLETED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // 8. reviews_cache
        db.execSQL("CREATE TABLE " + TBL_REVIEWS + " (" +
                COL_REVIEW_ID + " TEXT PRIMARY KEY, " +
                COL_REQUEST_ID + " TEXT, " +
                COL_REVIEWER_ID + " TEXT, " +
                COL_REVIEWER_NAME + " TEXT, " +
                COL_REVIEWEE_ID + " TEXT, " +
                COL_REVIEWEE_NAME + " TEXT, " +
                COL_RATING + " REAL, " +
                COL_COMMENT + " TEXT, " +
                COL_IMAGE_URI + " TEXT, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        // Saved houses table
        db.execSQL("CREATE TABLE " + TBL_SAVED_HOUSES + " (" +
                COL_USER_ID + " TEXT, " +
                COL_POST_ID + " TEXT, " +
                COL_SAVED_AT + " INTEGER, " +
                "PRIMARY KEY (" + COL_USER_ID + ", " + COL_POST_ID + ")" +
                ")");

        // Indexes
        db.execSQL("CREATE UNIQUE INDEX idx_users_phone ON " + TBL_USERS + "(" + COL_PHONE + ")");
        db.execSQL("CREATE INDEX idx_addr_owner ON " + TBL_VERIFIED_ADDRESSES + "(" + COL_OWNER_USER_ID + ")");
        db.execSQL("CREATE INDEX idx_ticket_user ON " + TBL_VERIFICATION_TICKETS + "(" + COL_USER_ID + ")");
        db.execSQL("CREATE INDEX idx_posts_user ON " + TBL_POSTS + "(" + COL_USER_ID + ")");
        db.execSQL("CREATE INDEX idx_posts_fingerprint ON " + TBL_POSTS + "(" + COL_ADDRESS_FINGERPRINT + ")");
        db.execSQL("CREATE INDEX idx_msgs_room ON " + TBL_MESSAGES + "(" + COL_ROOM_ID + ")");
        db.execSQL("CREATE INDEX idx_exchange_sender ON " + TBL_EXCHANGE + "(" + COL_SENDER_ID2 + ")");
        db.execSQL("CREATE INDEX idx_exchange_receiver ON " + TBL_EXCHANGE + "(" + COL_RECEIVER_ID + ")");
        db.execSQL("CREATE INDEX idx_review_reviewee ON " + TBL_REVIEWS + "(" + COL_REVIEWEE_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_VERIFIED_ADDRESSES);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_VERIFICATION_TICKETS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CHAT_ROOMS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_EXCHANGE);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_REVIEWS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_SAVED_HOUSES);
        onCreate(db);
    }
}
