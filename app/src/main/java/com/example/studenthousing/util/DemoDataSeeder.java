package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.local.ChatRoomDao;
import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoDataSeeder {
    private static final String TAG = "DemoDataSeeder";
    private static final long DAY = 24L * 3600 * 1000;
    private static final long HOUR = 3600 * 1000;

    public static void seedLocalAndRemote(Context ctx) {
        try {
            Context appCtx = ctx.getApplicationContext();
            UserDao userDao = new UserDao(appCtx);
            VerifiedAddressDao addressDao = new VerifiedAddressDao(appCtx);
            VerificationTicketDao ticketDao = new VerificationTicketDao(appCtx);
            PostDao postDao = new PostDao(appCtx);
            ExchangeDao exchangeDao = new ExchangeDao(appCtx);
            ReviewDao reviewDao = new ReviewDao(appCtx);
            MessageDao messageDao = new MessageDao(appCtx);
            ChatRoomDao chatRoomDao = new ChatRoomDao(appCtx);

            // Seed Users
            List<User> users = buildUsers();
            for (User u : users) {
                userDao.upsert(u);
            }

            // Seed Verified Addresses
            List<VerifiedAddress> addresses = buildAddresses();
            for (VerifiedAddress a : addresses) {
                addressDao.upsert(a);
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

            // Seed Exchange Requests
            List<ExchangeRequest> requests = buildRequests();
            for (ExchangeRequest r : requests) {
                exchangeDao.upsert(r);
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

            Log.d(TAG, "Local DB seeded successfully");

            // Sync to Firestore if online
            if (NetworkUtils.isOnline(appCtx)) {
                syncToFirestore(users, addresses, posts, requests, reviews, rooms, msgs);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding demo data: " + e.getMessage(), e);
        }
    }

    private static void syncToFirestore(List<User> users, List<VerifiedAddress> addresses,
                                        List<AccommodationPost> posts, List<ExchangeRequest> requests,
                                        List<Review> reviews, List<ChatRoom> rooms, List<Message> msgs) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Users
        for (User u : users) {
            if (u.getUserId() != null) {
                db.collection("users").document(u.getUserId()).set(u);
            }
        }

        // Verified Addresses
        for (VerifiedAddress a : addresses) {
            if (a.getAddressId() != null) {
                db.collection("verified_addresses").document(a.getAddressId()).set(a);
            }
        }

        // Posts
        for (AccommodationPost p : posts) {
            if (p.getPostId() != null) {
                db.collection("accommodation_posts").document(p.getPostId()).set(PostRemote.toPublicPostData(p));
            }
        }

        // Exchange Requests
        for (ExchangeRequest r : requests) {
            if (r.getRequestId() != null) {
                db.collection("exchange_requests").document(r.getRequestId()).set(r);
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
    }

    private static List<User> buildUsers() {
        long now = System.currentTimeMillis();
        List<User> list = new ArrayList<>();

        User u1 = new User();
        u1.setUserId("user_001");
        u1.setFamilyName("Nguyễn Văn A");
        u1.setPhone("0901234567");
        u1.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80");
        u1.setIdentityType(Constants.IDENTITY_STUDENT);
        u1.setIdentityVerified(true);
        u1.setIdentityVerifiedAt(now - 30 * DAY);
        u1.setUniversityCode("BK_HCM");
        u1.setUniversityName("Đại học Bách Khoa TP.HCM");
        u1.setResidenceVerified(true);
        u1.setResidenceVerifiedAt(now - 25 * DAY);
        u1.setFullyVerified(true);
        u1.setPermanentAddress("123 Lý Thường Kiệt, Phường 14, TP.HCM");
        u1.setPermanentCityCode(79);
        u1.setPermanentWardCode(27256);
        u1.setPermanentCityName("TP. Hồ Chí Minh");
        u1.setPermanentWardName("Phường 14");
        u1.setPhoneVerified(true);
        u1.setVerified(true);
        u1.setCreatedAt(now - 35 * DAY);
        u1.setUpdatedAt(now - 25 * DAY);
        list.add(u1);

        User u2 = new User();
        u2.setUserId("user_002");
        u2.setFamilyName("Trần Thị B");
        u2.setPhone("0912345678");
        u2.setAvatarUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80");
        u2.setIdentityType(Constants.IDENTITY_CITIZEN);
        u2.setIdentityVerified(true);
        u2.setIdentityVerifiedAt(now - 40 * DAY);
        u2.setCitizenIdHash(sha256("079123456789"));
        u2.setResidenceVerified(true);
        u2.setResidenceVerifiedAt(now - 35 * DAY);
        u2.setFullyVerified(true);
        u2.setPermanentAddress("45 Thảo Điền, Phường Thảo Điền, TP.HCM");
        u2.setPermanentCityCode(79);
        u2.setPermanentWardCode(26881);
        u2.setPermanentCityName("TP. Hồ Chí Minh");
        u2.setPermanentWardName("Phường Thảo Điền");
        u2.setPhoneVerified(true);
        u2.setVerified(true);
        u2.setCreatedAt(now - 45 * DAY);
        u2.setUpdatedAt(now - 35 * DAY);
        list.add(u2);

        User u3 = new User();
        u3.setUserId("user_003");
        u3.setFamilyName("Lê Văn C");
        u3.setPhone("0923456789");
        u3.setAvatarUrl("https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=300&q=80");
        u3.setIdentityType(Constants.IDENTITY_STUDENT);
        u3.setIdentityVerified(true);
        u3.setIdentityVerifiedAt(now - 15 * DAY);
        u3.setUniversityCode("KHTN_HCM");
        u3.setUniversityName("ĐH Khoa học Tự nhiên TP.HCM");
        u3.setResidenceVerified(false);
        u3.setFullyVerified(false);
        u3.setPermanentAddress("227 Nguyễn Văn Cừ, Phường 4, TP.HCM");
        u3.setPermanentCityCode(79);
        u3.setPermanentWardCode(27301);
        u3.setPermanentCityName("TP. Hồ Chí Minh");
        u3.setPermanentWardName("Phường 4");
        u3.setPhoneVerified(true);
        u3.setVerified(true);
        u3.setCreatedAt(now - 20 * DAY);
        u3.setUpdatedAt(now - 15 * DAY);
        list.add(u3);

        User u4 = new User();
        u4.setUserId("user_004");
        u4.setFamilyName("Phạm Minh D");
        u4.setPhone("0934567890");
        u4.setAvatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80");
        u4.setIdentityType(Constants.IDENTITY_STUDENT);
        u4.setIdentityVerified(true);
        u4.setIdentityVerifiedAt(now - 50 * DAY);
        u4.setUniversityCode("USSH_2");
        u4.setUniversityName("ĐH Quốc Tế TP.HCM");
        u4.setResidenceVerified(true);
        u4.setResidenceVerifiedAt(now - 45 * DAY);
        u4.setFullyVerified(true);
        u4.setPermanentAddress("12 Linh Trung, Phường Linh Trung, TP.HCM");
        u4.setPermanentCityCode(79);
        u4.setPermanentWardCode(26866);
        u4.setPermanentCityName("TP. Hồ Chí Minh");
        u4.setPermanentWardName("Phường Linh Trung");
        u4.setPhoneVerified(true);
        u4.setVerified(true);
        u4.setCreatedAt(now - 60 * DAY);
        u4.setUpdatedAt(now - 45 * DAY);
        list.add(u4);

        User u5 = new User();
        u5.setUserId("user_005");
        u5.setFamilyName("Hoàng Thị E");
        u5.setPhone("0945678901");
        u5.setAvatarUrl("https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=300&q=80");
        u5.setIdentityType(Constants.IDENTITY_STUDENT);
        u5.setIdentityVerified(true);
        u5.setIdentityVerifiedAt(now - 10 * DAY);
        u5.setUniversityCode("UEH_HCM");
        u5.setUniversityName("Đại học Kinh tế TP.HCM");
        u5.setResidenceVerified(true);
        u5.setResidenceVerifiedAt(now - 8 * DAY);
        u5.setFullyVerified(true);
        u5.setPermanentAddress("482 Điện Biên Phủ, Phường 22, TP.HCM");
        u5.setPermanentCityCode(79);
        u5.setPermanentWardCode(26908);
        u5.setPermanentCityName("TP. Hồ Chí Minh");
        u5.setPermanentWardName("Phường 22");
        u5.setPhoneVerified(true);
        u5.setVerified(true);
        u5.setCreatedAt(now - 15 * DAY);
        u5.setUpdatedAt(now - 8 * DAY);
        list.add(u5);

        User ua = new User();
        ua.setUserId("user_0123456789");
        ua.setFamilyName("Gia đình User A");
        ua.setPhone("0123456789");
        ua.setPassword("123456");
        ua.setRole("PARENT");
        ua.setPhoneVerified(true);
        ua.setVerified(true);
        ua.setResidenceVerified(true);
        ua.setCreatedAt(now - 30 * DAY);
        ua.setUpdatedAt(now - 30 * DAY);
        list.add(ua);

        User ub = new User();
        ub.setUserId("user_0987654321");
        ub.setFamilyName("Gia đình User B");
        ub.setPhone("0987654321");
        ub.setPassword("654321");
        ub.setRole("PARENT");
        ub.setPhoneVerified(true);
        ub.setVerified(true);
        ub.setResidenceVerified(true);
        ub.setCreatedAt(now - 30 * DAY);
        ub.setUpdatedAt(now - 30 * DAY);
        list.add(ub);

        return list;
    }

    private static List<VerifiedAddress> buildAddresses() {
        long now = System.currentTimeMillis();
        List<VerifiedAddress> list = new ArrayList<>();

        VerifiedAddress a1 = new VerifiedAddress();
        a1.setAddressId("addr_001");
        a1.setCityCode(79);
        a1.setWardCode(27256);
        a1.setStreetNormalized("ly thuong kiet");
        a1.setHouseNumberNorm("123");
        a1.setFullAddress("123 Lý Thường Kiệt, Phường 14, TP.HCM");
        a1.setOwnerUserId("user_001");
        a1.setVerificationTicketId("ticket_001");
        a1.setVerifiedAt(now - 30 * DAY);
        a1.setStatus(Constants.ADDR_VERIFIED);
        a1.setFingerprint(AddressFingerprintUtil.build(79, 27256, "ly thuong kiet", "123"));
        list.add(a1);

        VerifiedAddress a2 = new VerifiedAddress();
        a2.setAddressId("addr_002");
        a2.setCityCode(79);
        a2.setWardCode(26881);
        a2.setStreetNormalized("thao dien");
        a2.setHouseNumberNorm("45");
        a2.setFullAddress("45 Thảo Điền, Phường Thảo Điền, TP.HCM");
        a2.setOwnerUserId("user_002");
        a2.setVerificationTicketId("ticket_002");
        a2.setVerifiedAt(now - 35 * DAY);
        a2.setStatus(Constants.ADDR_VERIFIED);
        a2.setFingerprint(AddressFingerprintUtil.build(79, 26881, "thao dien", "45"));
        list.add(a2);

        VerifiedAddress a4 = new VerifiedAddress();
        a4.setAddressId("addr_004");
        a4.setCityCode(79);
        a4.setWardCode(26866);
        a4.setStreetNormalized("linh trung");
        a4.setHouseNumberNorm("12");
        a4.setFullAddress("12 Linh Trung, Phường Linh Trung, TP.HCM");
        a4.setOwnerUserId("user_004");
        a4.setVerificationTicketId("ticket_004");
        a4.setVerifiedAt(now - 45 * DAY);
        a4.setStatus(Constants.ADDR_VERIFIED);
        a4.setFingerprint(AddressFingerprintUtil.build(79, 26866, "linh trung", "12"));
        list.add(a4);

        VerifiedAddress a5 = new VerifiedAddress();
        a5.setAddressId("addr_005");
        a5.setCityCode(79);
        a5.setWardCode(26863);
        a5.setStreetNormalized("linh xuan");
        a5.setHouseNumberNorm("88");
        a5.setFullAddress("88 Linh Xuân, Phường Linh Xuân, TP.HCM");
        a5.setOwnerUserId("user_004");
        a5.setVerificationTicketId("ticket_005");
        a5.setVerifiedAt(now - 40 * DAY);
        a5.setStatus(Constants.ADDR_VERIFIED);
        a5.setFingerprint(AddressFingerprintUtil.build(79, 26863, "linh xuan", "88"));
        list.add(a5);

        VerifiedAddress a6 = new VerifiedAddress();
        a6.setAddressId("addr_006");
        a6.setCityCode(79);
        a6.setWardCode(26908);
        a6.setStreetNormalized("dien bien phu");
        a6.setHouseNumberNorm("482");
        a6.setFullAddress("482 Điện Biên Phủ, Phường 22, TP.HCM");
        a6.setOwnerUserId("user_005");
        a6.setVerificationTicketId("ticket_006");
        a6.setVerifiedAt(now - 10 * DAY);
        a6.setStatus(Constants.ADDR_VERIFIED);
        a6.setFingerprint(AddressFingerprintUtil.build(79, 26908, "dien bien phu", "482"));
        list.add(a6);

        VerifiedAddress a7 = new VerifiedAddress();
        a7.setAddressId("addr_007");
        a7.setCityCode(79);
        a7.setWardCode(27121);
        a7.setStreetNormalized("truong chinh");
        a7.setHouseNumberNorm("154");
        a7.setFullAddress("154 Trường Chinh, Phường 13, TP.HCM");
        a7.setOwnerUserId("user_001");
        a7.setVerificationTicketId("ticket_007");
        a7.setVerifiedAt(now - 5 * DAY);
        a7.setStatus(Constants.ADDR_VERIFIED);
        a7.setFingerprint(AddressFingerprintUtil.build(79, 27121, "truong chinh", "154"));
        list.add(a7);

        VerifiedAddress a8 = new VerifiedAddress();
        a8.setAddressId("addr_008");
        a8.setCityCode(79);
        a8.setWardCode(27481);
        a8.setStreetNormalized("nguyen huu tho");
        a8.setHouseNumberNorm("85");
        a8.setFullAddress("85 Nguyễn Hữu Thọ, Phường Tân Hưng, TP.HCM");
        a8.setOwnerUserId("user_002");
        a8.setVerificationTicketId("ticket_008");
        a8.setVerifiedAt(now - 12 * DAY);
        a8.setStatus(Constants.ADDR_VERIFIED);
        a8.setFingerprint(AddressFingerprintUtil.build(79, 27481, "nguyen huu tho", "85"));
        list.add(a8);

        return list;
    }

    private static List<VerificationTicket> buildTickets() {
        List<VerificationTicket> list = new ArrayList<>();
        return list;
    }

    // ===================================================================
    // BUILD POSTS
    // ===================================================================
    private static List<AccommodationPost> buildPosts() {
        long now = System.currentTimeMillis();
        List<AccommodationPost> list = new ArrayList<>();

        AccommodationPost p1 = new AccommodationPost();
        p1.setPostId("post_001");
        p1.setUserId("user_001");
        p1.setFamilyName("Nguyễn Văn A");
        p1.setTitle("Căn nhà gần Đại học Bách Khoa");
        p1.setAddress("123 Lý Thường Kiệt, Phường 14, TP.HCM");
        p1.setStreetName("Lý Thường Kiệt");
        p1.setHouseNumber("123");
        p1.setAddressFingerprint(AddressFingerprintUtil.build(79, 27256, "ly thuong kiet", "123"));
        p1.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p1.setCityCode(79);
        p1.setWardCode(27256);
        p1.setCityName("TP. Hồ Chí Minh");
        p1.setWardName("Phường 14");
        p1.setDescription("Phòng đầy đủ tiện nghi, có máy lạnh, gác xép, ban công thoáng mát, thích hợp cho sinh viên Bách Khoa, Kinh Tế. Giờ giấc tự do, khóa vân tay an toàn.");
        p1.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p1.setUtilities(Arrays.asList("Máy lạnh", "Wifi", "Tủ lạnh", "Chỗ để xe", "Khóa vân tay"));
        p1.setStatus(Constants.POST_AVAILABLE);
        p1.setCreatedAt(now - DAY);
        p1.setUpdatedAt(now - DAY);
        list.add(p1);

        AccommodationPost p2 = new AccommodationPost();
        p2.setPostId("post_002");
        p2.setUserId("user_002");
        p2.setFamilyName("Trần Thị B");
        p2.setTitle("Căn hộ studio Thảo Điền view sông thoáng mát");
        p2.setAddress("45 Thảo Điền, Phường Thảo Điền, TP. Thủ Đức, TP.HCM");
        p2.setStreetName("Thảo Điền");
        p2.setHouseNumber("45");
        p2.setAddressFingerprint(AddressFingerprintUtil.build(79, 26881, "thao dien", "45"));
        p2.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p2.setCityCode(79);
        p2.setWardCode(26881);
        p2.setCityName("TP. Hồ Chí Minh");
        p2.setWardName("Phường Thảo Điền");
        p2.setDescription("Studio cao cấp full nội thất, giờ giấc tự do, an ninh 24/7, có hồ bơi và phòng gym. Thích hợp cho người đi làm hoặc sinh viên cần yên tĩnh.");
        p2.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p2.setUtilities(Arrays.asList("Máy lạnh", "Thang máy", "Bảo vệ 24/7", "Máy giặt", "Hồ bơi", "Wifi"));
        p2.setStatus(Constants.POST_AVAILABLE);
        p2.setCreatedAt(now - 2 * DAY);
        p2.setUpdatedAt(now - 2 * DAY);
        list.add(p2);

        AccommodationPost p3 = new AccommodationPost();
        p3.setPostId("post_003");
        p3.setUserId("user_003");
        p3.setFamilyName("Lê Văn C");
        p3.setTitle("Phòng ghép sinh viên gần ĐH KHXH&NV / KHTN");
        p3.setAddress("227 Nguyễn Văn Cừ, Phường 4, TP.HCM");
        p3.setStreetName("Nguyễn Văn Cừ");
        p3.setHouseNumber("227");
        p3.setAddressFingerprint(AddressFingerprintUtil.build(79, 27301, "nguyen van cu", "227"));
        p3.setAddressVerificationStatus(Constants.ADDR_UNVERIFIED);
        p3.setCityCode(79);
        p3.setWardCode(27301);
        p3.setCityName("TP. Hồ Chí Minh");
        p3.setWardName("Phường 4");
        p3.setDescription("Tìm bạn nam ở ghép sạch sẽ, văn minh. Phòng rộng rãi có ban công, gần bến xe buýt và các trường ĐH trung tâm.");
        p3.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80"
        ));
        p3.setUtilities(Arrays.asList("Wifi", "Chỗ để xe", "Máy giặt chung", "Sân phơi"));
        p3.setStatus(Constants.POST_AVAILABLE);
        p3.setCreatedAt(now - 3 * DAY);
        p3.setUpdatedAt(now - 3 * DAY);
        list.add(p3);

        AccommodationPost p4 = new AccommodationPost();
        p4.setPostId("post_004");
        p4.setUserId("user_004");
        p4.setFamilyName("Phạm Minh D");
        p4.setTitle("Nhà nguyên căn Linh Trung Thủ Đức gần ĐHQG");
        p4.setAddress("12 Linh Trung, Phường Linh Trung, TP. Thủ Đức, TP.HCM");
        p4.setStreetName("Linh Trung");
        p4.setHouseNumber("12");
        p4.setAddressFingerprint(AddressFingerprintUtil.build(79, 26866, "linh trung", "12"));
        p4.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p4.setCityCode(79);
        p4.setWardCode(26866);
        p4.setCityName("TP. Hồ Chí Minh");
        p4.setWardName("Phường Linh Trung");
        p4.setDescription("Nhà 1 trệt 1 lầu, 2 phòng ngủ, 2 WC, bếp rộng rãi. Thích hợp cho nhóm 4-6 sinh viên ĐHQG ở chung. Hẻm xe hơi ra vào thoải mái.");
        p4.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80"
        ));
        p4.setUtilities(Arrays.asList("Máy lạnh", "Tủ lạnh", "Sân xe", "Bếp riêng", "Máy giặt"));
        p4.setStatus(Constants.POST_EXCHANGED);
        p4.setCreatedAt(now - 5 * DAY);
        p4.setUpdatedAt(now - 4 * DAY);
        list.add(p4);

        AccommodationPost p5 = new AccommodationPost();
        p5.setPostId("post_005");
        p5.setUserId("user_004");
        p5.setFamilyName("Phạm Minh D");
        p5.setTitle("Căn nhà cao cấp Linh Xuân gần Làng Đại Học");
        p5.setAddress("88 Linh Xuân, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        p5.setStreetName("Linh Xuân");
        p5.setHouseNumber("88");
        p5.setAddressFingerprint(AddressFingerprintUtil.build(79, 26863, "linh xuan", "88"));
        p5.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p5.setCityCode(79);
        p5.setWardCode(26863);
        p5.setCityName("TP. Hồ Chí Minh");
        p5.setWardName("Phường Linh Xuân");
        p5.setDescription("Phòng mới xây gần ĐHQG, có gác lửng cao không đụng đầu, thiết kế hiện đại, yên tĩnh tuyệt đối để học tập.");
        p5.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
        ));
        p5.setUtilities(Arrays.asList("Máy lạnh", "Wifi", "Bảo vệ", "Cửa từ", "Gác lửng"));
        p5.setStatus(Constants.POST_AVAILABLE);
        p5.setCreatedAt(now - 6 * DAY);
        p5.setUpdatedAt(now - 6 * DAY);
        list.add(p5);

        AccommodationPost p6 = new AccommodationPost();
        p6.setPostId("post_006");
        p6.setUserId("user_005");
        p6.setFamilyName("Hoàng Thị E");
        p6.setTitle("Căn hộ dịch vụ gần HUTECH / UEF");
        p6.setAddress("482 Điện Biên Phủ, Phường 22, TP.HCM");
        p6.setStreetName("Điện Biên Phủ");
        p6.setHouseNumber("482");
        p6.setAddressFingerprint(AddressFingerprintUtil.build(79, 26908, "dien bien phu", "482"));
        p6.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p6.setCityCode(79);
        p6.setWardCode(26908);
        p6.setCityName("TP. Hồ Chí Minh");
        p6.setWardName("Phường 22");
        p6.setDescription("Căn hộ dịch vụ mới 100%, có ban công đón nắng, đầy đủ sofa, tủ lạnh, bếp từ. Đi bộ 5 phút tới HUTECH và UEF.");
        p6.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p6.setUtilities(Arrays.asList("Máy lạnh", "Tủ lạnh", "Thang máy", "Bếp từ", "Ban công", "Wifi tốc độ cao"));
        p6.setStatus(Constants.POST_AVAILABLE);
        p6.setCreatedAt(now - 12 * HOUR);
        p6.setUpdatedAt(now - 12 * HOUR);
        list.add(p6);

        AccommodationPost p7 = new AccommodationPost();
        p7.setPostId("post_007");
        p7.setUserId("user_001");
        p7.setFamilyName("Nguyễn Văn A");
        p7.setTitle("Căn nhà gần Sân Bay & ĐH Mở");
        p7.setAddress("154 Trường Chinh, Phường 13, TP.HCM");
        p7.setStreetName("Trường Chinh");
        p7.setHouseNumber("154");
        p7.setAddressFingerprint(AddressFingerprintUtil.build(79, 27121, "truong chinh", "154"));
        p7.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p7.setCityCode(79);
        p7.setWardCode(27121);
        p7.setCityName("TP. Hồ Chí Minh");
        p7.setWardName("Phường 13");
        p7.setDescription("Căn nhà khép kín, an ninh tốt, gần chợ và trạm xe buýt. Không gian sạch sẽ, nước nóng năng lượng mặt trời.");
        p7.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80"
        ));
        p7.setUtilities(Arrays.asList("Máy lạnh", "Wifi", "Chỗ để xe", "Nước nóng NLMT"));
        p7.setStatus(Constants.POST_AVAILABLE);
        p7.setCreatedAt(now - 4 * HOUR);
        p7.setUpdatedAt(now - 4 * HOUR);
        list.add(p7);

        AccommodationPost p8 = new AccommodationPost();
        p8.setPostId("post_008");
        p8.setUserId("user_002");
        p8.setFamilyName("Trần Thị B");
        p8.setTitle("Chung cư mini gần ĐH Tôn Đức Thắng / RMIT");
        p8.setAddress("85 Nguyễn Hữu Thọ, Phường Tân Hưng, TP.HCM");
        p8.setStreetName("Nguyễn Hữu Thọ");
        p8.setHouseNumber("85");
        p8.setAddressFingerprint(AddressFingerprintUtil.build(79, 27481, "nguyen huu tho", "85"));
        p8.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p8.setCityCode(79);
        p8.setWardCode(27481);
        p8.setCityName("TP. Hồ Chí Minh");
        p8.setWardName("Phường Tân Hưng");
        p8.setDescription("Căn hộ chung cư mini nội thất gỗ sang trọng, có quản lý tòa nhà, giờ giấc tự do 24/7. Rất tiện di chuyển sang TĐT và RMIT.");
        p8.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p8.setUtilities(Arrays.asList("Máy lạnh", "Thang máy", "Thẻ từ an ninh", "Bãi xe rộng", "Máy giặt"));
        p8.setStatus(Constants.POST_AVAILABLE);
        p8.setCreatedAt(now - 8 * HOUR);
        p8.setUpdatedAt(now - 8 * HOUR);
        list.add(p8);

        return list;
    }

    private static String sha256(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ===================================================================
    // BUILD REQUESTS
    // ===================================================================
    private static List<ExchangeRequest> buildRequests() {
        long now = System.currentTimeMillis();
        List<ExchangeRequest> list = new ArrayList<>();

        ExchangeRequest r1 = new ExchangeRequest();
        r1.setRequestId("req_001");
        r1.setSenderId("user_001");
        r1.setReceiverId("user_003");
        r1.setSenderPostId("post_001");
        r1.setReceiverPostId("post_003");
        r1.setChatRoomId("room_001");
        r1.setExchangePeriod("6 tháng");
        r1.setStartDate(now + 7 * DAY);
        r1.setEndDate(now + 187 * DAY);
        r1.setMessage("Mình muốn đổi nhà để tiện di chuyển sang cơ sở học hơn.");
        r1.setStatus(Constants.REQ_PENDING);
        r1.setCreatedAt(now - DAY);
        list.add(r1);

        ExchangeRequest r2 = new ExchangeRequest();
        r2.setRequestId("req_002");
        r2.setSenderId("user_002");
        r2.setReceiverId("user_001");
        r2.setSenderPostId("post_002");
        r2.setReceiverPostId("post_001");
        r2.setChatRoomId("room_002");
        r2.setExchangePeriod("1 năm");
        r2.setStartDate(now + 2 * DAY);
        r2.setEndDate(now + 367 * DAY);
        r2.setMessage("Nhà bạn gần trường quá, mình muốn trao đổi.");
        r2.setStatus(Constants.REQ_EXCHANGING);
        r2.setCreatedAt(now - 3 * DAY);
        r2.setRespondedAt(now - 2 * DAY);
        list.add(r2);

        ExchangeRequest r3 = new ExchangeRequest();
        r3.setRequestId("req_003");
        r3.setSenderId("user_003");
        r3.setReceiverId("user_005");
        r3.setSenderPostId("post_003");
        r3.setReceiverPostId("post_006");
        r3.setChatRoomId("room_003");
        r3.setExchangePeriod("3 tháng");
        r3.setStartDate(now - 90 * DAY);
        r3.setEndDate(now - DAY);
        r3.setMessage("Đổi nhà ngắn hạn kỳ thực tập.");
        r3.setStatus(Constants.REQ_COMPLETED);
        r3.setCreatedAt(now - 95 * DAY);
        r3.setRespondedAt(now - 94 * DAY);
        r3.setCompletedAt(now - DAY);
        list.add(r3);

        return list;
    }

    private static List<Review> buildReviews() {
        long now = System.currentTimeMillis();
        List<Review> list = new ArrayList<>();

        Review rev1 = new Review();
        rev1.setReviewId("review_001");
        rev1.setRequestId("req_003");
        rev1.setReviewerId("user_003");
        rev1.setReviewerName("Lê Văn C");
        rev1.setRevieweeId("user_005");
        rev1.setRevieweeName("Hoàng Thị E");
        rev1.setRating(5.0);
        rev1.setComment("Trao đổi nhà rất suôn sẻ, nhà sạch sẽ đúng mô tả, chủ nhà thân thiện hỗ trợ nhiệt tình.");
        rev1.setImageUrls(Arrays.asList("https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"));
        rev1.setCreatedAt(now - DAY);
        list.add(rev1);

        Review rev2 = new Review();
        rev2.setReviewId("review_002");
        rev2.setRequestId("req_003");
        rev2.setReviewerId("user_005");
        rev2.setReviewerName("Hoàng Thị E");
        rev2.setRevieweeId("user_003");
        rev2.setRevieweeName("Lê Văn C");
        rev2.setRating(4.8);
        rev2.setComment("Bạn C giữ nhà rất gọn gàng sạch sẽ sau khi bàn giao. Rất hài lòng.");
        rev2.setImageUrls(Collections.emptyList());
        rev2.setCreatedAt(now - DAY);
        list.add(rev2);

        return list;
    }

    private static List<ChatRoom> buildChatRooms() {
        long now = System.currentTimeMillis();
        List<ChatRoom> list = new ArrayList<>();

        ChatRoom r1 = new ChatRoom();
        r1.setRoomId("room_001");
        r1.setParticipantIds(Arrays.asList("user_001", "user_003"));
        r1.setLastMessage("Mình muốn đổi nhà để tiện di chuyển sang cơ sở học hơn.");
        r1.setLastMessageTime(now - DAY);
        r1.setPostId("post_001");
        r1.setPostTitle("Căn nhà gần Đại học Bách Khoa");
        r1.setLastSenderId("user_001");
        r1.setCreatedAt(now - DAY);
        list.add(r1);

        ChatRoom r2 = new DataSeederChatRoomHelper().buildRoom2(now);
        list.add(r2);

        ChatRoom r3 = new ChatRoom();
        r3.setRoomId("room_003");
        r3.setParticipantIds(Arrays.asList("user_003", "user_005"));
        r3.setLastMessage("Cảm ơn bạn A, chúc bạn kỳ học mới thật tốt nhé!");
        r3.setLastMessageTime(now - DAY);
        r3.setPostId("post_003");
        r3.setPostTitle("Phòng ghép sinh viên gần ĐH KHXH&NV / KHTN");
        r3.setLastSenderId("user_005");
        r3.setCreatedAt(now - 95 * DAY);
        list.add(r3);

        return list;
    }

    private static class DataSeederChatRoomHelper {
        ChatRoom buildRoom2(long now) {
            ChatRoom r2 = new ChatRoom();
            r2.setRoomId("room_002");
            r2.setParticipantIds(Arrays.asList("user_002", "user_001"));
            r2.setLastMessage("Chị đã xác nhận ký hợp đồng trao đổi rồi nha em.");
            r2.setLastMessageTime(now - 3 * HOUR);
            r2.setPostId("post_002");
            r2.setPostTitle("Căn hộ studio Thảo Điền view sông thoáng mát");
            r2.setLastSenderId("user_002");
            r2.setCreatedAt(now - 3 * DAY);
            return r2;
        }
    }

    private static List<Message> buildMessages() {
        long now = System.currentTimeMillis();
        List<Message> list = new ArrayList<>();
        // Room 1
        Message m1 = new Message();
        m1.setMessageId("msg_001");
        m1.setRoomId("room_001");
        m1.setSenderId("user_001");
        m1.setContent("Chào bạn, mình thấy nhà bạn rất phù hợp.");
        m1.setType("TEXT");
        m1.setSentAt(now - DAY - 10 * HOUR);
        m1.setRead(true);
        list.add(m1);

        Message m2 = new Message();
        m2.setMessageId("msg_002");
        m2.setRoomId("room_001");
        m2.setSenderId("user_003");
        m2.setContent("Chào bạn, nhà mình ở khu vực trung tâm rất thuận tiện.");
        m2.setType("TEXT");
        m2.setSentAt(now - DAY - 9 * HOUR);
        m2.setRead(true);
        list.add(m2);

        Message m3 = new Message();
        m3.setMessageId("msg_003");
        m3.setRoomId("room_001");
        m3.setSenderId("user_001");
        m3.setContent("Mình muốn đổi nhà để tiện di chuyển sang cơ sở học hơn.");
        m3.setType("TEXT");
        m3.setSentAt(now - DAY);
        m3.setRead(true);
        list.add(m3);

        // Room 2
        Message m4 = new Message();
        m4.setMessageId("msg_004");
        m4.setRoomId("room_002");
        m4.setSenderId("user_001");
        m4.setContent("Chị ơi em muốn đổi nhà ạ.");
        m4.setType("TEXT");
        m4.setSentAt(now - 3 * DAY);
        m4.setRead(true);
        list.add(m4);

        Message m9 = new Message();
        m9.setMessageId("msg_009");
        m9.setRoomId("room_002");
        m9.setSenderId("user_002");
        m9.setContent("Chị đã xác nhận ký hợp đồng trao đổi rồi nha em.");
        m9.setType("TEXT");
        m9.setSentAt(now - 3 * HOUR);
        m9.setRead(true);
        list.add(m9);

        // Room 3
        Message m10 = new Message();
        m10.setMessageId("msg_010");
        m10.setRoomId("room_003");
        m10.setSenderId("user_001");
        m10.setContent("Cảm ơn chị E, đợt trao đổi 3 tháng qua rất thuận lợi ạ!");
        m10.setType("TEXT");
        m10.setSentAt(now - 2 * DAY);
        m10.setRead(true);
        list.add(m10);

        Message m11 = new Message();
        m11.setMessageId("msg_011");
        m11.setRoomId("room_003");
        m11.setSenderId("user_005");
        m11.setContent("Cảm ơn bạn A, chúc bạn kỳ học mới thật tốt nhé!");
        m11.setType("TEXT");
        m11.setSentAt(now - DAY);
        m11.setRead(true);
        list.add(m11);

        return list;
    }
}
