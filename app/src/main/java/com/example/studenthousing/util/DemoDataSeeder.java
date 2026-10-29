package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.local.ChatRoomDao;
import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.remote.PostRemote;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoDataSeeder {
    private static final String TAG = "DemoDataSeeder";
    private static final long DAY = 24L * 3600 * 1000;
    private static final long HOUR = 3600 * 1000;

    // ===== Utility codes chuẩn theo utilities.json =====
    private static final String U_WIFI = "WIFI";
    private static final String U_PARKING = "PARKING";
    private static final String U_AC = "AIR_CONDITIONER";
    private static final String U_WASHER = "WASHER";
    private static final String U_FRIDGE = "FRIDGE";
    private static final String U_PRIVATE_BATH = "PRIVATE_BATHROOM";
    private static final String U_SECURITY = "SECURITY";
    private static final String U_ELEVATOR = "ELEVATOR";
    private static final String U_PET = "PET_ALLOWED";

    public static void seedLocalAndRemote(Context ctx) {
        try {
            Context appCtx = ctx.getApplicationContext();
            UserDao userDao = new UserDao(appCtx);
            VerificationTicketDao ticketDao = new VerificationTicketDao(appCtx);
            PostDao postDao = new PostDao(appCtx);
            ReviewDao reviewDao = new ReviewDao(appCtx);
            MessageDao messageDao = new MessageDao(appCtx);
            ChatRoomDao chatRoomDao = new ChatRoomDao(appCtx);
            com.example.studenthousing.data.local.VerifiedAddressDao verifiedAddressDao = new com.example.studenthousing.data.local.VerifiedAddressDao(appCtx);

            // Seed Users
            List<User> users = buildUsers();
            for (User u : users) {
                userDao.upsert(u);
                if (u.isResidenceVerified() && u.getPermanentAddress() != null && !u.getPermanentAddress().isEmpty()) {
                    verifiedAddressDao.upsert(new com.example.studenthousing.data.model.VerifiedAddress(
                            "addr_" + u.getUserId(),
                            u.getPermanentAddress(),
                            u.getUserId(),
                            u.getPermanentCityCode(),
                            u.getPermanentWardCode(),
                            "seed_ticket_" + u.getUserId()
                    ));
                }
            }

            // Seed Verification Tickets
            List<VerificationTicket> tickets = buildTickets();
            for (VerificationTicket t : tickets) {
                ticketDao.upsert(t);
            }

            // Seed Posts
            List<AccommodationPost> posts = buildPosts();
            for (AccommodationPost p : posts) {
                postDao.upsert(p);
            }

            // Seed Reviews
            List<Review> reviews = buildReviews();
            for (Review rv : reviews) {
                reviewDao.upsert(rv);
            }

            // Seed Chat Rooms & Messages
            List<ChatRoom> rooms = buildChatRooms();
            for (ChatRoom cr : rooms) {
                chatRoomDao.upsert(cr);
            }

            List<Message> msgs = buildMessages();
            for (Message m : msgs) {
                messageDao.upsert(m);
            }

            // Seed Campuses (từ assets/campuses.json — nếu có)
            List<Campus> campuses = buildCampusesFromAssets(appCtx);
            Log.d(TAG, "Loaded " + campuses.size() + " campuses from assets");

            Log.d(TAG, "Local DB seeded successfully");

            // Sync to Firestore if online
            if (NetworkUtils.isOnline(appCtx)) {
                syncToFirestore(users, posts, reviews, rooms, msgs, campuses);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding demo data: " + e.getMessage(), e);
        }
    }

    private static void syncToFirestore(List<User> users,
                                        List<AccommodationPost> posts,
                                        List<Review> reviews,
                                        List<ChatRoom> rooms,
                                        List<Message> msgs,
                                        List<Campus> campuses) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Users
        for (User u : users) {
            if (u.getUserId() != null) {
                db.collection("users").document(u.getUserId()).set(u);
            }
        }

        // Posts
        for (AccommodationPost p : posts) {
            if (p.getPostId() != null) {
                db.collection("accommodation_posts").document(p.getPostId()).set(PostRemote.toPostData(p));
            }
        }

        // Reviews
        for (Review rv : reviews) {
            if (rv.getReviewId() != null) {
                db.collection("reviews").document(rv.getReviewId()).set(rv);
            }
        }

        // Chat Rooms
        for (ChatRoom cr : rooms) {
            if (cr.getRoomId() != null) {
                db.collection("chat_rooms").document(cr.getRoomId()).set(cr);
            }
        }

        // Messages
        for (Message m : msgs) {
            if (m.getMessageId() != null && m.getRoomId() != null) {
                db.collection("chat_rooms").document(m.getRoomId())
                        .collection("messages").document(m.getMessageId()).set(m);
            }
        }

        // Campuses
        for (Campus c : campuses) {
            if (c.getCampusCode() != null) {
                db.collection("campuses").document(c.getCampusCode()).set(c);
            }
        }
    }

    // ===================================================================
    // CAMPUSES (parse assets/campuses.json)
    // ===================================================================
    private static List<Campus> buildCampusesFromAssets(Context ctx) {
        List<Campus> list = new ArrayList<>();
        try (InputStream is = ctx.getAssets().open("campuses.json")) {
            byte[] buf = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buf);
            String json = new String(buf, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Campus c = new Campus();
                c.setCampusCode(o.optString("campusCode"));
                c.setName(o.optString("name"));
                c.setFullName(o.optString("fullName"));
                c.setType(o.optString("type"));
                c.setSatellite(o.optBoolean("isSatellite"));
                c.setCategory(o.optString("category"));
                c.setDistrict(o.optString("district"));
                c.setLat(o.optDouble("lat"));
                c.setLon(o.optDouble("lon"));
                c.setOsmId(o.optLong("osmId"));
                c.setOsmType(o.optString("osmType"));
                list.add(c);
            }
        } catch (Exception e) {
            Log.w(TAG, "Không đọc được assets/campuses.json: " + e.getMessage());
        }
        return list;
    }

    // ===================================================================
    // USERS
    // ===================================================================
    private static List<User> buildUsers() {
        long now = System.currentTimeMillis();
        List<User> list = new ArrayList<>();

        // u1 — Bách Khoa (campus 45), Phường Chợ Quán (27301)
        User u1 = new User();
        u1.setUserId("user_001");
        u1.setFamilyName("Nguyễn Văn A");
        u1.setPhone("0901234567");
        u1.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80");
        u1.setIdentityType(Constants.IDENTITY_STUDENT);
        u1.setIdentityVerified(true);
        u1.setIdentityVerifiedAt(now - 30 * DAY);
        u1.setUniversityCode("45");
        u1.setUniversityName("Trường Đại học Bách khoa, Đại học Quốc gia TP.HCM");
        u1.setResidenceVerified(true);
        u1.setResidenceVerifiedAt(now - 25 * DAY);
        u1.setPermanentAddress("123 Lý Thường Kiệt, Phường Chợ Quán, TP.HCM");
        u1.setPermanentCityCode(79);
        u1.setPermanentWardCode(27301); // Phường Chợ Quán
        u1.setPermanentCityName("TP. Hồ Chí Minh");
        u1.setPermanentWardName("Phường Chợ Quán");
        u1.setCreatedAt(now - 35 * DAY);
        u1.setUpdatedAt(now - 25 * DAY);
        list.add(u1);

        // u2 — Trần Thị B, Phường An Khánh (27094)
        User u2 = new User();
        u2.setUserId("user_002");
        u2.setFamilyName("Trần Thị B");
        u2.setPhone("0912345678");
        u2.setAvatarUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80");
        u2.setIdentityType(Constants.IDENTITY_CITIZEN);
        u2.setIdentityVerified(true);
        u2.setIdentityVerifiedAt(now - 40 * DAY);
        u2.setResidenceVerified(true);
        u2.setResidenceVerifiedAt(now - 35 * DAY);
        u2.setPermanentAddress("45 Thảo Điền, Phường An Khánh, TP. Thủ Đức, TP.HCM");
        u2.setPermanentCityCode(79);
        u2.setPermanentWardCode(27094); // Phường An Khánh
        u2.setPermanentCityName("TP. Hồ Chí Minh");
        u2.setPermanentWardName("Phường An Khánh");
        u2.setCreatedAt(now - 45 * DAY);
        u2.setUpdatedAt(now - 35 * DAY);
        list.add(u2);

        // u3 — KHTN (campus 59/60), Phường Chợ Quán (27301)
        User u3 = new User();
        u3.setUserId("user_003");
        u3.setFamilyName("Lê Văn C");
        u3.setPhone("0923456789");
        u3.setAvatarUrl("https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=300&q=80");
        u3.setIdentityType(Constants.IDENTITY_STUDENT);
        u3.setIdentityVerified(true);
        u3.setIdentityVerifiedAt(now - 15 * DAY);
        u3.setUniversityCode("59");
        u3.setUniversityName("Trường Đại học Khoa học Tự nhiên - ĐHQG TP.HCM");
        u3.setResidenceVerified(false);
        u3.setPermanentAddress("227 Nguyễn Văn Cừ, Phường Chợ Quán, TP.HCM");
        u3.setPermanentCityCode(79);
        u3.setPermanentWardCode(27301); // Phường Chợ Quán
        u3.setPermanentCityName("TP. Hồ Chí Minh");
        u3.setPermanentWardName("Phường Chợ Quán");
        u3.setCreatedAt(now - 20 * DAY);
        u3.setUpdatedAt(now - 15 * DAY);
        list.add(u3);

        // u4 — ĐH Quốc Tế (campus 84), Phường Linh Xuân (26800)
        User u4 = new User();
        u4.setUserId("user_004");
        u4.setFamilyName("Phạm Minh D");
        u4.setPhone("0934567890");
        u4.setAvatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80");
        u4.setIdentityType(Constants.IDENTITY_STUDENT);
        u4.setIdentityVerified(true);
        u4.setIdentityVerifiedAt(now - 50 * DAY);
        u4.setUniversityCode("84");
        u4.setUniversityName("Trường Đại học Quốc tế - ĐHQG TP.HCM");
        u4.setResidenceVerified(true);
        u4.setResidenceVerifiedAt(now - 45 * DAY);
        u4.setPermanentAddress("12 Linh Xuân, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        u4.setPermanentCityCode(79);
        u4.setPermanentWardCode(26800); // Phường Linh Xuân
        u4.setPermanentCityName("TP. Hồ Chí Minh");
        u4.setPermanentWardName("Phường Linh Xuân");
        u4.setCreatedAt(now - 60 * DAY);
        u4.setUpdatedAt(now - 45 * DAY);
        list.add(u4);

        // u5 — UEH (campus 67/68/69/70/71/72), Phường An Khánh (27094)
        User u5 = new User();
        u5.setUserId("user_005");
        u5.setFamilyName("Hoàng Thị E");
        u5.setPhone("0945678901");
        u5.setAvatarUrl("https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=300&q=80");
        u5.setIdentityType(Constants.IDENTITY_STUDENT);
        u5.setIdentityVerified(true);
        u5.setIdentityVerifiedAt(now - 10 * DAY);
        u5.setUniversityCode("72");
        u5.setUniversityName("Đại học Kinh tế TP.HCM - Cơ sở A");
        u5.setResidenceVerified(true);
        u5.setResidenceVerifiedAt(now - 8 * DAY);
        u5.setPermanentAddress("482 Điện Biên Phủ, Phường An Khánh, TP.HCM");
        u5.setPermanentCityCode(79);
        u5.setPermanentWardCode(27094); // Phường An Khánh
        u5.setPermanentCityName("TP. Hồ Chí Minh");
        u5.setPermanentWardName("Phường An Khánh");
        u5.setCreatedAt(now - 15 * DAY);
        u5.setUpdatedAt(now - 8 * DAY);
        list.add(u5);

        User ua = new User();
        ua.setUserId("user_0123456789");
        ua.setFamilyName("Gia đình User A");
        ua.setPhone("0123456789");
        ua.setResidenceVerified(true);
        ua.setCreatedAt(now - 30 * DAY);
        ua.setUpdatedAt(now - 30 * DAY);
        list.add(ua);

        User ub = new User();
        ub.setUserId("user_0987654321");
        ub.setFamilyName("Gia đình User B");
        ub.setPhone("0987654321");
        ub.setResidenceVerified(true);
        ub.setCreatedAt(now - 30 * DAY);
        ub.setUpdatedAt(now - 30 * DAY);
        list.add(ub);

        return list;
    }

    private static List<VerificationTicket> buildTickets() {
        return new ArrayList<>();
    }

    // ===================================================================
    // POSTS
    // ===================================================================
    private static List<AccommodationPost> buildPosts() {
        long now = System.currentTimeMillis();
        List<AccommodationPost> list = new ArrayList<>();

        // p1 — Chợ Quán (27301)
        AccommodationPost p1 = new AccommodationPost();
        p1.setPostId("post_001");
        p1.setUserId("user_001");
        p1.setFamilyName("Nguyễn Văn A");
        p1.setTitle("Căn nhà gần Đại học Bách Khoa");
        p1.setAddress("123 Lý Thường Kiệt, Phường Chợ Quán, TP.HCM");
        p1.setStreetName("Lý Thường Kiệt");
        p1.setHouseNumber("123");
        p1.setAddressValidationStatus(Constants.ADDR_VALID);
        p1.setCityCode(79);
        p1.setWardCode(27301);
        p1.setCityName("TP. Hồ Chí Minh");
        p1.setWardName("Phường Chợ Quán");
        p1.setDescription("Phòng đầy đủ tiện nghi, có máy lạnh, gác xép, ban công thoáng mát, thích hợp cho sinh viên Bách Khoa, Kinh Tế. Giờ giấc tự do, khóa vân tay an toàn.");
        p1.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p1.setUtilities(Arrays.asList(U_AC, U_WIFI, U_FRIDGE, U_PARKING, U_SECURITY));
        p1.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("45", 0.6, false), // ĐH Bách Khoa
                new NearbyCampus("68", 1.2, false), // ĐH Kinh tế TP.HCM cơ sở C
                new NearbyCampus("69", 1.4, false), // ĐH Kinh tế TP.HCM cơ sở B
                new NearbyCampus("103", 1.8, false), // ĐH Y Dược TP.HCM
                new NearbyCampus("92", 2.2, false), // ĐH Sư Phạm TP.HCM
                new NearbyCampus("90", 2.5, false)  // ĐH Sài Gòn
        ));
        p1.setStatus(Constants.POST_AVAILABLE);
        p1.setCreatedAt(now - DAY);
        p1.setUpdatedAt(now - DAY);
        list.add(p1);

        // p2 — An Khánh (27094)
        AccommodationPost p2 = new AccommodationPost();
        p2.setPostId("post_002");
        p2.setUserId("user_002");
        p2.setFamilyName("Trần Thị B");
        p2.setTitle("Căn hộ studio Thảo Điền view sông thoáng mát");
        p2.setAddress("45 Thảo Điền, Phường An Khánh, TP. Thủ Đức, TP.HCM");
        p2.setStreetName("Thảo Điền");
        p2.setHouseNumber("45");
        p2.setCityCode(79);
        p2.setWardCode(27094);
        p2.setCityName("TP. Hồ Chí Minh");
        p2.setWardName("Phường An Khánh");
        p2.setDescription("Studio cao cấp full nội thất, giờ giấc tự do, an ninh 24/7, có hồ bơi và phòng gym. Thích hợp cho người đi làm hoặc sinh viên cần yên tĩnh.");
        p2.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p2.setUtilities(Arrays.asList(U_AC, U_ELEVATOR, U_SECURITY, U_WASHER, U_WIFI, U_PARKING));
        p2.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("129", 1.1, false), // ĐH Văn Hóa TP.HCM
                new NearbyCampus("125", 1.5, false), // Saigon International Univ
                new NearbyCampus("51", 2.8, false),  // HUTECH
                new NearbyCampus("85", 3.2, false)   // ĐH Quốc tế Hồng Bàng
        ));
        p2.setStatus(Constants.POST_AVAILABLE);
        p2.setCreatedAt(now - 2 * DAY);
        p2.setUpdatedAt(now - 2 * DAY);
        list.add(p2);

        // p3 — Chợ Quán (27301)
        AccommodationPost p3 = new AccommodationPost();
        p3.setPostId("post_003");
        p3.setUserId("user_003");
        p3.setFamilyName("Lê Văn C");
        p3.setTitle("Phòng ghép sinh viên gần ĐH KHXH&NV / KHTN");
        p3.setAddress("227 Nguyễn Văn Cừ, Phường Chợ Quán, TP.HCM");
        p3.setStreetName("Nguyễn Văn Cừ");
        p3.setHouseNumber("227");
        p3.setCityCode(79);
        p3.setWardCode(27301);
        p3.setCityName("TP. Hồ Chí Minh");
        p3.setWardName("Phường Chợ Quán");
        p3.setDescription("Tìm bạn nam ở ghép sạch sẽ, văn minh. Phòng rộng rãi có ban công, gần bến xe buýt và các trường ĐH trung tâm.");
        p3.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80"
        ));
        p3.setUtilities(Arrays.asList(U_WIFI, U_PARKING, U_WASHER));
        p3.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("59", 0.3, false), // ĐH KHTN (CS1)
                new NearbyCampus("90", 0.5, false), // ĐH Sài Gòn
                new NearbyCampus("92", 0.7, false), // ĐH Sư Phạm TP.HCM
                new NearbyCampus("72", 1.2, false), // ĐH Kinh tế UEH (Cơ sở A)
                new NearbyCampus("105", 2.1, false) // ĐH Y Khoa Phạm Ngọc Thạch
        ));
        p3.setStatus(Constants.POST_AVAILABLE);
        p3.setCreatedAt(now - 3 * DAY);
        p3.setUpdatedAt(now - 3 * DAY);
        list.add(p3);

        // p4 — Linh Xuân (26800)
        AccommodationPost p4 = new AccommodationPost();
        p4.setPostId("post_004");
        p4.setUserId("user_004");
        p4.setFamilyName("Phạm Minh D");
        p4.setTitle("Nhà nguyên căn Linh Xuân Thủ Đức gần ĐHQG");
        p4.setAddress("12 Linh Xuân, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        p4.setStreetName("Linh Xuân");
        p4.setHouseNumber("12");
        p4.setCityCode(79);
        p4.setWardCode(26800);
        p4.setCityName("TP. Hồ Chí Minh");
        p4.setWardName("Phường Linh Xuân");
        p4.setDescription("Nhà 1 trệt 1 lầu, 2 phòng ngủ, 2 WC, bếp rộng rãi. Thích hợp cho nhóm 4-6 sinh viên ĐHQG ở chung. Hẻm xe hơi ra vào thoải mái.");
        p4.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80"
        ));
        p4.setUtilities(Arrays.asList(U_AC, U_FRIDGE, U_PARKING, U_WASHER, U_PRIVATE_BATH));
        p4.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("44", 1.2, false), // ĐH Bách Khoa (CS Dĩ An)
                new NearbyCampus("60", 1.5, false), // ĐH KHTN (CS Linh Trung)
                new NearbyCampus("52", 1.8, false), // ĐH Công nghệ Thông tin (UIT)
                new NearbyCampus("84", 2.0, false), // ĐH Quốc Tế
                new NearbyCampus("82", 2.5, false)  // ĐH Nông Lâm
        ));
        p4.setStatus(Constants.POST_EXCHANGED);
        p4.setCreatedAt(now - 5 * DAY);
        p4.setUpdatedAt(now - 4 * DAY);
        list.add(p4);

        // p5 — Linh Xuân (26800)
        AccommodationPost p5 = new AccommodationPost();
        p5.setPostId("post_005");
        p5.setUserId("user_004");
        p5.setFamilyName("Phạm Minh D");
        p5.setTitle("Căn nhà cao cấp Linh Xuân gần Làng Đại Học");
        p5.setAddress("88 Linh Xuân, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        p5.setStreetName("Linh Xuân");
        p5.setHouseNumber("88");
        p5.setCityCode(79);
        p5.setWardCode(26800);
        p5.setCityName("TP. Hồ Chí Minh");
        p5.setWardName("Phường Linh Xuân");
        p5.setDescription("Phòng mới xây gần ĐHQG, có gác lửng cao không đụng đầu, thiết kế hiện đại, yên tĩnh tuyệt đối để học tập.");
        p5.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
        ));
        p5.setUtilities(Arrays.asList(U_AC, U_WIFI, U_SECURITY, U_PARKING));
        p5.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("44", 1.0, false), // ĐH Bách Khoa (CS Dĩ An)
                new NearbyCampus("60", 1.3, false), // ĐH KHTN (CS Linh Trung)
                new NearbyCampus("52", 1.6, false), // ĐH Công nghệ Thông tin (UIT)
                new NearbyCampus("84", 1.9, false), // ĐH Quốc Tế
                new NearbyCampus("78", 2.4, false)  // ĐH Ngân Hàng TP.HCM
        ));
        p5.setStatus(Constants.POST_AVAILABLE);
        p5.setCreatedAt(now - 6 * DAY);
        p5.setUpdatedAt(now - 6 * DAY);
        list.add(p5);

        // p6 — An Khánh (27094)
        AccommodationPost p6 = new AccommodationPost();
        p6.setPostId("post_006");
        p6.setUserId("user_005");
        p6.setFamilyName("Hoàng Thị E");
        p6.setTitle("Căn hộ dịch vụ gần HUTECH / UEF");
        p6.setAddress("482 Điện Biên Phủ, Phường An Khánh, TP.HCM");
        p6.setStreetName("Điện Biên Phủ");
        p6.setHouseNumber("482");
        p6.setCityCode(79);
        p6.setWardCode(27094);
        p6.setCityName("TP. Hồ Chí Minh");
        p6.setWardName("Phường An Khánh");
        p6.setDescription("Căn hộ dịch vụ mới 100%, có ban công đón nắng, đầy đủ sofa, tủ lạnh, bếp từ. Đi bộ 5 phút tới HUTECH và UEF.");
        p6.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p6.setUtilities(Arrays.asList(U_AC, U_FRIDGE, U_ELEVATOR, U_WIFI, U_PRIVATE_BATH));
        p6.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("51", 0.4, false), // HUTECH
                new NearbyCampus("85", 0.8, false), // ĐH Quốc tế Hồng Bàng
                new NearbyCampus("123", 1.2, false), // ĐH Ngoại Thương CS2
                new NearbyCampus("129", 1.8, false)  // ĐH Văn Hóa
        ));
        p6.setStatus(Constants.POST_AVAILABLE);
        p6.setCreatedAt(now - 12 * HOUR);
        p6.setUpdatedAt(now - 12 * HOUR);
        list.add(p6);

        // p7 — Tân Sơn Nhất (26968)
        AccommodationPost p7 = new AccommodationPost();
        p7.setPostId("post_007");
        p7.setUserId("user_001");
        p7.setFamilyName("Nguyễn Văn A");
        p7.setTitle("Căn nhà gần Sân Bay & ĐH Mở");
        p7.setAddress("154 Trường Chinh, Phường Tân Sơn Nhất, TP.HCM");
        p7.setStreetName("Trường Chinh");
        p7.setHouseNumber("154");
        p7.setCityCode(79);
        p7.setWardCode(26968);
        p7.setCityName("TP. Hồ Chí Minh");
        p7.setWardName("Phường Tân Sơn Nhất");
        p7.setDescription("Căn nhà khép kín, an ninh tốt, gần chợ và trạm xe buýt. Không gian sạch sẽ, nước nóng năng lượng mặt trời.");
        p7.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80"
        ));
        p7.setUtilities(Arrays.asList(U_AC, U_WIFI, U_PARKING, U_SECURITY));
        p7.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("113", 1.0, false), // Học viện Hàng không
                new NearbyCampus("74", 1.5, false),  // ĐH Mở TP.HCM
                new NearbyCampus("55", 2.2, false),  // ĐH Gia Định
                new NearbyCampus("53", 2.6, false)   // ĐH Công Thương
        ));
        p7.setStatus(Constants.POST_AVAILABLE);
        p7.setCreatedAt(now - 4 * HOUR);
        p7.setUpdatedAt(now - 4 * HOUR);
        list.add(p7);

        // p8 — Tân Hưng (27475)
        AccommodationPost p8 = new AccommodationPost();
        p8.setPostId("post_008");
        p8.setUserId("user_002");
        p8.setFamilyName("Trần Thị B");
        p8.setTitle("Chung cư mini gần ĐH Tôn Đức Thắng / RMIT");
        p8.setAddress("85 Nguyễn Hữu Thọ, Phường Tân Hưng, TP.HCM");
        p8.setStreetName("Nguyễn Hữu Thọ");
        p8.setHouseNumber("85");
        p8.setCityCode(79);
        p8.setWardCode(27475);
        p8.setCityName("TP. Hồ Chí Minh");
        p8.setWardName("Phường Tân Hưng");
        p8.setDescription("Căn hộ chung cư mini nội thất gỗ sang trọng, có quản lý tòa nhà, giờ giấc tự do 24/7. Rất tiện di chuyển sang TĐT và RMIT.");
        p8.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p8.setUtilities(Arrays.asList(U_AC, U_ELEVATOR, U_SECURITY, U_PARKING, U_WASHER));
        p8.setNearbyCampuses(Arrays.asList(
                new NearbyCampus("98", 0.6, false), // ĐH Tôn Đức Thắng
                new NearbyCampus("88", 1.2, false), // RMIT
                new NearbyCampus("80", 1.8, false), // ĐH Nguyễn Tất Thành
                new NearbyCampus("94", 2.3, false), // ĐH Tài chính Marketing
                new NearbyCampus("1", 2.8, false)   // CĐ Bách Khoa Nam Sài Gòn
        ));
        p8.setStatus(Constants.POST_AVAILABLE);
        p8.setCreatedAt(now - 8 * HOUR);
        p8.setUpdatedAt(now - 8 * HOUR);
        list.add(p8);

        return list;
    }

    // ===================================================================
    // REVIEWS
    // ===================================================================
    private static List<Review> buildReviews() {
        long now = System.currentTimeMillis();
        List<Review> list = new ArrayList<>();

        Review rev1 = new Review();
        rev1.setReviewId("review_001");
        rev1.setReviewerId("user_003");
        rev1.setReviewerName("Lê Văn C");
        rev1.setRevieweeId("user_005");
        rev1.setRevieweeName("Hoàng Thị E");
        rev1.setRating(5.0);
        rev1.setComment("Trao đổi nhà rất suôn sẻ, nhà sạch sẽ đúng mô tả, chủ nhà thân thiện hỗ trợ nhiệt tình.");
        rev1.setImageUrls(Collections.singletonList(
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"));
        rev1.setCreatedAt(now - DAY);
        rev1.setUpdatedAt(now - DAY);
        list.add(rev1);

        Review rev2 = new Review();
        rev2.setReviewId("review_002");
        rev2.setReviewerId("user_005");
        rev2.setReviewerName("Hoàng Thị E");
        rev2.setRevieweeId("user_003");
        rev2.setRevieweeName("Lê Văn C");
        rev2.setRating(4.8);
        rev2.setComment("Bạn C giữ nhà rất gọn gàng sạch sẽ sau khi bàn giao. Rất hài lòng.");
        rev2.setImageUrls(Collections.emptyList());
        rev2.setCreatedAt(now - DAY);
        rev2.setUpdatedAt(now - DAY);
        list.add(rev2);

        // Reviews for user_001 (Nguyễn Văn A)
        Review rev3 = new Review();
        rev3.setReviewId("review_003");
        rev3.setReviewerId("user_002");
        rev3.setReviewerName("Trần Thị B");
        rev3.setRevieweeId("user_001");
        rev3.setRevieweeName("Nguyễn Văn A");
        rev3.setRating(5.0);
        rev3.setComment("Bạn A rất uy tín, trao đổi nhà đúng thời hạn, phòng ốc bàn giao ngăn nắp sạch sẽ.");
        rev3.setImageUrls(Collections.emptyList());
        rev3.setCreatedAt(now - 2 * DAY);
        rev3.setUpdatedAt(now - 2 * DAY);
        list.add(rev3);

        Review rev4 = new Review();
        rev4.setReviewId("review_004");
        rev4.setReviewerId("user_004");
        rev4.setReviewerName("Phạm Minh D");
        rev4.setRevieweeId("user_001");
        rev4.setRevieweeName("Nguyễn Văn A");
        rev4.setRating(4.7);
        rev4.setComment("Chủ nhà rất nhiệt tình hỗ trợ, thông tin nhà rõ ràng và chính xác.");
        rev4.setImageUrls(Collections.emptyList());
        rev4.setCreatedAt(now - 5 * DAY);
        rev4.setUpdatedAt(now - 5 * DAY);
        list.add(rev4);

        // Reviews for user_002 (Trần Thị B)
        Review rev5 = new Review();
        rev5.setReviewId("review_005");
        rev5.setReviewerId("user_001");
        rev5.setReviewerName("Nguyễn Văn A");
        rev5.setRevieweeId("user_002");
        rev5.setRevieweeName("Trần Thị B");
        rev5.setRating(4.9);
        rev5.setComment("Chị B rất dễ thương, hỗ trợ mọi thủ tục nhanh gọn, phòng đẹp y hình.");
        rev5.setImageUrls(Collections.emptyList());
        rev5.setCreatedAt(now - 3 * DAY);
        rev5.setUpdatedAt(now - 3 * DAY);
        list.add(rev5);

        Review rev6 = new Review();
        rev6.setReviewId("review_006");
        rev6.setReviewerId("user_003");
        rev6.setReviewerName("Lê Văn C");
        rev6.setRevieweeId("user_002");
        rev6.setRevieweeName("Trần Thị B");
        rev6.setRating(5.0);
        rev6.setComment("Phòng Thảo Điền ở rất thích, an ninh và tiện ích đầy đủ.");
        rev6.setImageUrls(Collections.emptyList());
        rev6.setCreatedAt(now - 7 * DAY);
        rev6.setUpdatedAt(now - 7 * DAY);
        list.add(rev6);

        // Reviews for user_004 (Phạm Minh D)
        Review rev7 = new Review();
        rev7.setReviewId("review_007");
        rev7.setReviewerId("user_001");
        rev7.setReviewerName("Nguyễn Văn A");
        rev7.setRevieweeId("user_004");
        rev7.setRevieweeName("Phạm Minh D");
        rev7.setRating(4.6);
        rev7.setComment("Anh D hướng dẫn nhiệt tình, vị trí nhà rất gần cụm ĐHQG.");
        rev7.setImageUrls(Collections.emptyList());
        rev7.setCreatedAt(now - 4 * DAY);
        rev7.setUpdatedAt(now - 4 * DAY);
        list.add(rev7);

        return list;
    }

    // ===================================================================
    // CHAT ROOMS
    // ===================================================================
    private static List<ChatRoom> buildChatRooms() {
        long now = System.currentTimeMillis();
        List<ChatRoom> list = new ArrayList<>();

        ChatRoom r1 = new ChatRoom();
        r1.setRoomId("room_001");
        r1.setParticipantIds(Arrays.asList("user_001", "user_003"));
        r1.setLastMessage("Mình muốn đổi nhà để tiện di chuyển sang cơ sở học hơn.");
        r1.setLastMessageTime(now - DAY);
        r1.setLastSenderId("user_001");
        r1.setCreatedAt(now - DAY);
        list.add(r1);

        ChatRoom r2 = new ChatRoom();
        r2.setRoomId("room_002");
        r2.setParticipantIds(Arrays.asList("user_002", "user_001"));
        r2.setLastMessage("Chị đã xác nhận ký hợp đồng trao đổi rồi nha em.");
        r2.setLastMessageTime(now - 3 * HOUR);
        r2.setLastSenderId("user_002");
        r2.setCreatedAt(now - 3 * DAY);
        list.add(r2);

        ChatRoom r3 = new ChatRoom();
        r3.setRoomId("room_003");
        r3.setParticipantIds(Arrays.asList("user_003", "user_005"));
        r3.setLastMessage("Cảm ơn bạn A, chúc bạn kỳ học mới thật tốt nhé!");
        r3.setLastMessageTime(now - DAY);
        r3.setLastSenderId("user_005");
        r3.setCreatedAt(now - 95 * DAY);
        list.add(r3);

        return list;
    }

    // ===================================================================
    // MESSAGES
    // ===================================================================
    private static List<Message> buildMessages() {
        long now = System.currentTimeMillis();
        List<Message> list = new ArrayList<>();

        // Room 1
        list.add(msg("msg_001", "room_001", "user_001",
                "Chào bạn, mình thấy nhà bạn rất phù hợp.", now - DAY - 10 * HOUR));
        list.add(msg("msg_002", "room_001", "user_003",
                "Chào bạn, nhà mình ở khu vực trung tâm rất thuận tiện.", now - DAY - 9 * HOUR));
        list.add(msg("msg_003", "room_001", "user_001",
                "Mình muốn đổi nhà để tiện di chuyển sang cơ sở học hơn.", now - DAY));

        // Room 2
        list.add(msg("msg_004", "room_002", "user_001",
                "Chị ơi em muốn đổi nhà ạ.", now - 3 * DAY));
        list.add(msg("msg_009", "room_002", "user_002",
                "Chị đã xác nhận ký hợp đồng trao đổi rồi nha em.", now - 3 * HOUR));

        // Room 3
        list.add(msg("msg_010", "room_003", "user_001",
                "Cảm ơn chị E, đợt trao đổi 3 tháng qua rất thuận lợi ạ!", now - 2 * DAY));
        list.add(msg("msg_011", "room_003", "user_005",
                "Cảm ơn bạn A, chúc bạn kỳ học mới thật tốt nhé!", now - DAY));

        return list;
    }

    private static Message msg(String id, String roomId, String senderId, String content, long sentAt) {
        Message m = new Message();
        m.setMessageId(id);
        m.setRoomId(roomId);
        m.setSenderId(senderId);
        m.setContent(content);
        m.setType("TEXT");
        m.setSentAt(sentAt);
        m.setRead(true);
        return m;
    }
}