package com.example.studenthousing.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

public class SessionManager {
    private static final String PREF_NAME = "StudentHousingSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_FAMILY_NAME = "userFamilyName";
    private static final String KEY_USER_AVATAR_URL = "userAvatarUrl";

    private final Context context;
    private final SharedPreferences pref;

    public SessionManager(Context ctx) {
        context = ctx.getApplicationContext();
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(String userId) {
        pref.edit().putBoolean(KEY_IS_LOGGED_IN, userId != null && !userId.isEmpty())
                .putString(KEY_USER_ID, userId).apply();
    }

    public void createLoginSession(com.example.studenthousing.data.model.User user) {
        createLoginSession(user == null ? null : user.getUserId());
        if (user != null) {
            pref.edit().putString(KEY_USER_PHONE, user.getPhone())
                    .putString(KEY_USER_FAMILY_NAME, user.getFamilyName())
                    .putString(KEY_USER_AVATAR_URL, user.getAvatarUrl()).apply();
        }
    }

    public String getUserPhone() { return pref.getString(KEY_USER_PHONE, null); }
    public String getUserFamilyName() { return pref.getString(KEY_USER_FAMILY_NAME, null); }
    public String getAvatarUrl() { return pref.getString(KEY_USER_AVATAR_URL, null); }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != null;
    }

    public String getUserId() {
        com.google.firebase.auth.FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        return pref.getString(KEY_USER_ID, null);
    }

    public void logout() {
        // ✅ FIX: phải lấy uid TRƯỚC khi signOut, nếu không sẽ không biết xóa data của ai.
        final String uid = getUserId();

        FirebaseAuth.getInstance().signOut();

        // Xóa data local của RIÊNG user này — KHÔNG đụng tới user khác trên cùng máy.
        if (uid != null && !uid.isEmpty()) {
            try {
                android.database.sqlite.SQLiteDatabase db =
                        com.example.studenthousing.data.local.DatabaseHelper
                                .getInstance(context).getWritableDatabase();

                String syncedWhere = "(" +
                        com.example.studenthousing.data.local.DatabaseHelper.COL_SYNC_STATUS
                        + " IS NULL OR "
                        + com.example.studenthousing.data.local.DatabaseHelper.COL_SYNC_STATUS
                        + " = 'SYNCED')";

                // POSTS — chỉ của user này
                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_POSTS,
                        "user_id = ? AND " + syncedWhere,
                        new String[]{ uid });

                // MESSAGES — chỉ tin nhắn do user này gửi
                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_MESSAGES,
                        "sender_id = ? AND " + syncedWhere,
                        new String[]{ uid });

                // REVIEWS — chỉ review do user này viết
                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_REVIEWS,
                        "reviewer_id = ? AND " + syncedWhere,
                        new String[]{ uid });

                // CHAT ROOMS — chỉ các room mà user này tham gia.
                // participant_ids lưu dạng JSON array text (vd: ["uid1","uid2"]) hoặc CSV,
                // dùng LIKE với dấu nháy kép để tránh match nhầm substring uid.
                String participantLike = "%\"" + uid + "\"%";
                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_CHAT_ROOMS,
                        "participant_ids LIKE ?",
                        new String[]{ participantLike });

                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_CHAT_ROOM_PARTICIPANTS,
                        "user_id = ?",
                        new String[]{ uid });

                // SAVED HOUSES — chỉ của user này
                db.delete(
                        com.example.studenthousing.data.local.DatabaseHelper.TBL_SAVED_HOUSES,
                        "user_id = ?",
                        new String[]{ uid });

            } catch (Exception ignored) {
                // Không để lỗi DB chặn logout.
            }
        }

        pref.edit().clear().apply();
    }

    public static String getUid(Context ctx) {
        return new SessionManager(ctx).getUserId();
    }
}