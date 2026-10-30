package com.example.studenthousing.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "studenthousing.db";
    private static final int DB_VERSION = 3; // ✅ Tăng version để trigger onUpgrade

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
    public static final String COL_CITY_CODE = "city_code";
    public static final String COL_WARD_CODE = "ward_code";
    public static final String COL_CITY_NAME = "city_name";
    public static final String COL_WARD_NAME = "ward_name";
    public static final String COL_STREET_NAME = "street_name";
    public static final String COL_HOUSE_NUMBER = "house_number";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_IMAGES = "images";
    public static final String COL_UTILITIES = "utilities";
    public static final String COL_STATUS = "status";
    public static final String COL_CACHED_AT = "cached_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_SYNC_STATUS = "sync_status";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_NEARBY_JSON = "nearby_json";

    // Table: verified_addresses
    public static final String TBL_VERIFIED_ADDRESSES = "verified_addresses";
    public static final String COL_ADDRESS_ID = "address_id";
    public static final String COL_FINGERPRINT = "fingerprint";
    public static final String COL_STREET_NORMALIZED = "street_normalized";
    public static final String COL_HOUSE_NUMBER_NORM = "house_number_norm";
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

    // Table: users_cache
    public static final String TBL_USERS = "users_cache";
    public static final String COL_PHONE = "phone";
    public static final String COL_PASSWORD = "password";
    public static final String COL_AVATAR_URL = "avatar_url";
    public static final String COL_IDENTITY_TYPE = "identity_type";
    public static final String COL_IDENTITY_VERIFIED = "identity_verified";
    public static final String COL_IDENTITY_VERIFIED_AT = "identity_verified_at";
    public static final String COL_UNIVERSITY_CODE = "university_code";
    public static final String COL_UNIVERSITY_NAME = "university_name";
    public static final String COL_CITIZEN_ID_HASH = "citizen_id_hash";
    // ✅ THÊM 3 CỘT MỚI
    public static final String COL_CITIZEN_ID_FRONT_URL = "citizen_id_front_url";
    public static final String COL_CITIZEN_ID_BACK_URL = "citizen_id_back_url";
    public static final String COL_STUDENT_CARD_URL = "student_card_url";

    public static final String COL_RESIDENCE_VERIFIED = "residence_verified";
    public static final String COL_RESIDENCE_VERIFIED_AT = "residence_verified_at";
    public static final String COL_FULLY_VERIFIED = "fully_verified";
    public static final String COL_PERMANENT_ADDRESS = "permanent_address";
    public static final String COL_PERMANENT_CITY_CODE = "permanent_city_code";
    public static final String COL_PERMANENT_WARD_CODE = "permanent_ward_code";
    public static final String COL_PERMANENT_CITY_NAME = "permanent_city_name";
    public static final String COL_PERMANENT_WARD_NAME = "permanent_ward_name";
    public static final String COL_PHONE_VERIFIED = "phone_verified";
    public static final String COL_IS_VERIFIED = "is_verified";

    // Table: chat_rooms_cache
    public static final String TBL_CHAT_ROOMS = "chat_rooms_cache";
    public static final String TBL_CHAT_ROOM_PARTICIPANTS = "chat_room_participants";
    public static final String COL_LAST_MESSAGE = "last_message";
    public static final String COL_LAST_MESSAGE_AT = "last_message_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL_USERS + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_PHONE + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_AVATAR_URL + " TEXT, " +
                COL_IDENTITY_TYPE + " TEXT DEFAULT 'NONE', " +
                COL_IDENTITY_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_IDENTITY_VERIFIED_AT + " INTEGER DEFAULT 0, " +
                COL_UNIVERSITY_CODE + " TEXT, " +
                COL_UNIVERSITY_NAME + " TEXT, " +
                COL_CITIZEN_ID_HASH + " TEXT, " +
                // ✅ THÊM 3 CỘT MỚI
                COL_CITIZEN_ID_FRONT_URL + " TEXT, " +
                COL_CITIZEN_ID_BACK_URL + " TEXT, " +
                COL_STUDENT_CARD_URL + " TEXT, " +
                COL_RESIDENCE_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_RESIDENCE_VERIFIED_AT + " INTEGER DEFAULT 0, " +
                COL_FULLY_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_PERMANENT_ADDRESS + " TEXT, " +
                COL_PERMANENT_CITY_CODE + " INTEGER DEFAULT 79, " +
                COL_PERMANENT_WARD_CODE + " INTEGER DEFAULT 0, " +
                COL_PERMANENT_CITY_NAME + " TEXT, " +
                COL_PERMANENT_WARD_NAME + " TEXT, " +
                COL_PHONE_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_IS_VERIFIED + " INTEGER DEFAULT 0, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        db.execSQL("CREATE TABLE " + TBL_VERIFIED_ADDRESSES + " (" +
                COL_ADDRESS_ID + " TEXT PRIMARY KEY, " +
                COL_FINGERPRINT + " TEXT UNIQUE, " +
                COL_CITY_CODE + " INTEGER DEFAULT 79, " +
                COL_WARD_CODE + " INTEGER, " +
                COL_STREET_NORMALIZED + " TEXT, " +
                COL_HOUSE_NUMBER_NORM + " TEXT, " +
                COL_FULL_ADDRESS + " TEXT, " +
                COL_OWNER_USER_ID + " TEXT, " +
                COL_VERIFICATION_TICKET_ID + " TEXT, " +
                COL_VERIFIED_AT + " INTEGER, " +
                COL_STATUS + " TEXT, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

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

        db.execSQL("CREATE TABLE " + TBL_POSTS + " (" +
                COL_POST_ID + " TEXT PRIMARY KEY, " +
                COL_USER_ID + " TEXT, " +
                COL_FAMILY_NAME + " TEXT, " +
                COL_TITLE + " TEXT, " +
                COL_ADDRESS + " TEXT, " +
                COL_ADDRESS_FINGERPRINT + " TEXT, " +
                COL_ADDRESS_VERIFICATION_STATUS + " TEXT DEFAULT 'UNVERIFIED', " +
                COL_CITY_CODE + " INTEGER DEFAULT 79, " +
                COL_WARD_CODE + " INTEGER, " +
                COL_CITY_NAME + " TEXT DEFAULT 'TP. Hồ Chí Minh', " +
                COL_WARD_NAME + " TEXT, " +
                COL_STREET_NAME + " TEXT, " +
                COL_HOUSE_NUMBER + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_IMAGES + " TEXT, " +
                COL_UTILITIES + " TEXT, " +
                COL_STATUS + " TEXT DEFAULT 'AVAILABLE', " +
                COL_CREATED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'SYNCED', " +
                COL_NEARBY_JSON + " TEXT, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

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

        db.execSQL("CREATE TABLE " + TBL_CHAT_ROOM_PARTICIPANTS + " (" +
                COL_ROOM_ID + " TEXT, " +
                "user_id TEXT, " +
                "PRIMARY KEY (" + COL_ROOM_ID + ", user_id)" +
                ")");

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

        db.execSQL("CREATE TABLE " + TBL_SAVED_HOUSES + " (" +
                COL_USER_ID + " TEXT, " +
                COL_POST_ID + " TEXT, " +
                COL_SAVED_AT + " INTEGER, " +
                "PRIMARY KEY (" + COL_USER_ID + ", " + COL_POST_ID + ")" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // ✅ Sửa: Chỉ Alter Table để thêm cột, KHÔNG gọi onCreate() để tránh lỗi "table already exists"
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TBL_USERS + " ADD COLUMN " + COL_CITIZEN_ID_FRONT_URL + " TEXT");
                db.execSQL("ALTER TABLE " + TBL_USERS + " ADD COLUMN " + COL_CITIZEN_ID_BACK_URL + " TEXT");
                db.execSQL("ALTER TABLE " + TBL_USERS + " ADD COLUMN " + COL_STUDENT_CARD_URL + " TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAllLocalTables(db);
        onCreate(db);
    }

    private void dropAllLocalTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_SAVED_HOUSES);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CHAT_ROOM_PARTICIPANTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CHAT_ROOMS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_VERIFICATION_TICKETS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_VERIFIED_ADDRESSES);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
    }
}