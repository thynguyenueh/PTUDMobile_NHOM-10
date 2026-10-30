package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.loader.CampusDataLoader;
import com.example.studenthousing.data.loader.WardsDataLoader;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.data.model.Ward;
import com.example.studenthousing.data.remote.PostRemote;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
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

            // Seed Users
            List<User> users = buildUsers(appCtx);
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
            List<Post> posts = buildPosts(appCtx);
            for (Post p : posts) {
                postDao.upsert(p);
            }

            Log.d(TAG, "Local DB seeded successfully");

            // Sync to Firestore if online
            if (NetworkUtils.isOnline(appCtx)) {
                syncToFirestore(users, addresses, posts);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding demo data: " + e.getMessage(), e);
        }
    }

    private static void syncToFirestore(List<User> users, List<VerifiedAddress> addresses,
                                        List<Post> posts) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (User u : users) {
            if (u.getUserId() != null) {
                db.collection("users").document(u.getUserId()).set(u);
            }
        }
        for (VerifiedAddress a : addresses) {
            if (a.getAddressId() != null) {
                db.collection("verified_addresses").document(a.getAddressId()).set(a);
            }
        }
        for (Post p : posts) {
            if (p.getPostId() != null) {
                db.collection("accommodation_posts").document(p.getPostId())
                        .set(PostRemote.toPublicPostData(p));
            }
        }
    }

    // ===================================================================
    // HELPERS — map JSON → field demo cần
    // ===================================================================
    private static void applyCampus(Context ctx, User u, String campusCode) {
        Campus c = CampusDataLoader.getByCode(ctx, campusCode);
        if (c != null) {
            u.setUniversityCode(c.getCampusCode());
            u.setUniversityName(c.getName());
        }
    }

    private static void applyWardToUser(Context ctx, User u, String wardName) {
        Ward w = WardsDataLoader.findByName(ctx, wardName);
        if (w != null) {
            u.setPermanentWardCode(w.getCode());
            u.setPermanentWardName(w.getName());
        }
    }

    private static void applyWardToPost(Context ctx, Post p, String wardName) {
        Ward w = WardsDataLoader.findByName(ctx, wardName);
        if (w != null) {
            p.setWardCode(w.getCode());
            p.setWardName(w.getName());
        }
    }

    // ===================================================================
    // BUILD USERS
    // ===================================================================
    private static List<User> buildUsers(Context ctx) {
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
        applyCampus(ctx, u1, "45"); // ĐH Bách Khoa - ĐHQG TP.HCM
        u1.setResidenceVerified(true);
        u1.setResidenceVerifiedAt(now - 25 * DAY);
        u1.setFullyVerified(true);
        u1.setPermanentAddress("123 Lý Thường Kiệt, Phường Chợ Quán, TP.HCM");
        u1.setPermanentCityCode(79);
        u1.setPermanentCityName("TP. Hồ Chí Minh");
        applyWardToUser(ctx, u1, "Phường Chợ Quán");
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
        u2.setPermanentAddress("45 Thảo Điền, Phường An Khánh, TP.HCM");
        u2.setPermanentCityCode(79);
        u2.setPermanentCityName("TP. Hồ Chí Minh");
        applyWardToUser(ctx, u2, "Phường An Khánh");
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
        applyCampus(ctx, u3, "59"); // ĐH Khoa học Tự nhiên - ĐHQG TP.HCM
        u3.setResidenceVerified(false);
        u3.setFullyVerified(false);
        u3.setPermanentAddress("227 Nguyễn Văn Cừ, Phường Chợ Quán, TP.HCM");
        u3.setPermanentCityCode(79);
        u3.setPermanentCityName("TP. Hồ Chí Minh");
        applyWardToUser(ctx, u3, "Phường Chợ Quán");
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
        applyCampus(ctx, u4, "84"); // ĐH Quốc tế - ĐHQG TP.HCM
        u4.setResidenceVerified(true);
        u4.setResidenceVerifiedAt(now - 45 * DAY);
        u4.setFullyVerified(true);
        u4.setPermanentAddress("12 Linh Trung, Phường Linh Xuân, TP.HCM");
        u4.setPermanentCityCode(79);
        u4.setPermanentCityName("TP. Hồ Chí Minh");
        applyWardToUser(ctx, u4, "Phường Linh Xuân");
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
        applyCampus(ctx, u5, "72"); // ĐH Kinh tế TP.HCM - Cơ sở A
        u5.setResidenceVerified(true);
        u5.setResidenceVerifiedAt(now - 8 * DAY);
        u5.setFullyVerified(true);
        u5.setPermanentAddress("482 Điện Biên Phủ, Phường Bình Thạnh, TP.HCM");
        u5.setPermanentCityCode(79);
        u5.setPermanentCityName("TP. Hồ Chí Minh");
        applyWardToUser(ctx, u5, "Phường Bình Thạnh");
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

    // ===================================================================
    // BUILD ADDRESSES
    // ===================================================================
    private static List<VerifiedAddress> buildAddresses() {
        long now = System.currentTimeMillis();
        List<VerifiedAddress> list = new ArrayList<>();

        VerifiedAddress a1 = new VerifiedAddress();
        a1.setAddressId("addr_001");
        a1.setCityCode(79);
        a1.setWardCode(27301);
        a1.setStreetNormalized("ly thuong kiet");
        a1.setHouseNumberNorm("123");
        a1.setFullAddress("123 Lý Thường Kiệt, Phường Chợ Quán, TP.HCM");
        a1.setOwnerUserId("user_001");
        a1.setVerificationTicketId("ticket_001");
        a1.setVerifiedAt(now - 30 * DAY);
        a1.setStatus(Constants.ADDR_VERIFIED);
        a1.setFingerprint(AddressFingerprintUtil.build(79, 27301, "ly thuong kiet", "123"));
        list.add(a1);

        VerifiedAddress a2 = new VerifiedAddress();
        a2.setAddressId("addr_002");
        a2.setCityCode(79);
        a2.setWardCode(27094);
        a2.setStreetNormalized("thao dien");
        a2.setHouseNumberNorm("45");
        a2.setFullAddress("45 Thảo Điền, Phường An Khánh, TP.HCM");
        a2.setOwnerUserId("user_002");
        a2.setVerificationTicketId("ticket_002");
        a2.setVerifiedAt(now - 35 * DAY);
        a2.setStatus(Constants.ADDR_VERIFIED);
        a2.setFingerprint(AddressFingerprintUtil.build(79, 27094, "thao dien", "45"));
        list.add(a2);

        VerifiedAddress a4 = new VerifiedAddress();
        a4.setAddressId("addr_004");
        a4.setCityCode(79);
        a4.setWardCode(26800);
        a4.setStreetNormalized("linh trung");
        a4.setHouseNumberNorm("12");
        a4.setFullAddress("12 Linh Trung, Phường Linh Xuân, TP.HCM");
        a4.setOwnerUserId("user_004");
        a4.setVerificationTicketId("ticket_004");
        a4.setVerifiedAt(now - 45 * DAY);
        a4.setStatus(Constants.ADDR_VERIFIED);
        a4.setFingerprint(AddressFingerprintUtil.build(79, 26800, "linh trung", "12"));
        list.add(a4);

        VerifiedAddress a5 = new VerifiedAddress();
        a5.setAddressId("addr_005");
        a5.setCityCode(79);
        a5.setWardCode(26800);
        a5.setStreetNormalized("linh xuan");
        a5.setHouseNumberNorm("88");
        a5.setFullAddress("88 Linh Xuân, Phường Linh Xuân, TP.HCM");
        a5.setOwnerUserId("user_004");
        a5.setVerificationTicketId("ticket_005");
        a5.setVerifiedAt(now - 40 * DAY);
        a5.setStatus(Constants.ADDR_VERIFIED);
        a5.setFingerprint(AddressFingerprintUtil.build(79, 26800, "linh xuan", "88"));
        list.add(a5);

        VerifiedAddress a6 = new VerifiedAddress();
        a6.setAddressId("addr_006");
        a6.setCityCode(79);
        a6.setWardCode(26929);
        a6.setStreetNormalized("dien bien phu");
        a6.setHouseNumberNorm("482");
        a6.setFullAddress("482 Điện Biên Phủ, Phường Bình Thạnh, TP.HCM");
        a6.setOwnerUserId("user_005");
        a6.setVerificationTicketId("ticket_006");
        a6.setVerifiedAt(now - 10 * DAY);
        a6.setStatus(Constants.ADDR_VERIFIED);
        a6.setFingerprint(AddressFingerprintUtil.build(79, 26929, "dien bien phu", "482"));
        list.add(a6);

        VerifiedAddress a7 = new VerifiedAddress();
        a7.setAddressId("addr_007");
        a7.setCityCode(79);
        a7.setWardCode(26968);
        a7.setStreetNormalized("truong chinh");
        a7.setHouseNumberNorm("154");
        a7.setFullAddress("154 Trường Chinh, Phường Tân Sơn Nhất, TP.HCM");
        a7.setOwnerUserId("user_001");
        a7.setVerificationTicketId("ticket_007");
        a7.setVerifiedAt(now - 5 * DAY);
        a7.setStatus(Constants.ADDR_VERIFIED);
        a7.setFingerprint(AddressFingerprintUtil.build(79, 26968, "truong chinh", "154"));
        list.add(a7);

        VerifiedAddress a8 = new VerifiedAddress();
        a8.setAddressId("addr_008");
        a8.setCityCode(79);
        a8.setWardCode(27475);
        a8.setStreetNormalized("nguyen huu tho");
        a8.setHouseNumberNorm("85");
        a8.setFullAddress("85 Nguyễn Hữu Thọ, Phường Tân Hưng, TP.HCM");
        a8.setOwnerUserId("user_002");
        a8.setVerificationTicketId("ticket_008");
        a8.setVerifiedAt(now - 12 * DAY);
        a8.setStatus(Constants.ADDR_VERIFIED);
        a8.setFingerprint(AddressFingerprintUtil.build(79, 27475, "nguyen huu tho", "85"));
        list.add(a8);

        return list;
    }

    private static List<VerificationTicket> buildTickets() {
        return new ArrayList<>();
    }

    // ===================================================================
    // BUILD POSTS
    // ===================================================================
    private static List<Post> buildPosts(Context ctx) {
        long now = System.currentTimeMillis();
        List<Post> list = new ArrayList<>();

        Post p1 = new Post();
        p1.setPostId("post_001");
        p1.setUserId("user_001");
        p1.setFamilyName("Nguyễn Văn A");
        p1.setTitle("Căn nhà gần Đại học Bách Khoa");
        p1.setAddress("123 Lý Thường Kiệt, Phường Chợ Quán, TP.HCM");
        p1.setStreetName("Lý Thường Kiệt");
        p1.setHouseNumber("123");
        p1.setAddressFingerprint(AddressFingerprintUtil.build(79, 27301, "ly thuong kiet", "123"));
        p1.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p1.setCityCode(79);
        p1.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p1, "Phường Chợ Quán");
        p1.setDescription("Phòng đầy đủ tiện nghi, có máy lạnh, gác xép, ban công thoáng mát, thích hợp cho sinh viên Bách Khoa, Kinh Tế. Giờ giấc tự do, khóa vân tay an toàn.");
        p1.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p1.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.WIFI,
                UtilityCodes.FRIDGE,
                UtilityCodes.PARKING
        ));
        p1.setStatus(Constants.POST_AVAILABLE);
        p1.setCreatedAt(now - DAY);
        p1.setUpdatedAt(now - DAY);
        list.add(p1);

        Post p2 = new Post();
        p2.setPostId("post_002");
        p2.setUserId("user_002");
        p2.setFamilyName("Trần Thị B");
        p2.setTitle("Căn hộ studio Thảo Điền view sông thoáng mát");
        p2.setAddress("45 Thảo Điền, Phường An Khánh, TP. Thủ Đức, TP.HCM");
        p2.setStreetName("Thảo Điền");
        p2.setHouseNumber("45");
        p2.setAddressFingerprint(AddressFingerprintUtil.build(79, 27094, "thao dien", "45"));
        p2.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p2.setCityCode(79);
        p2.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p2, "Phường An Khánh");
        p2.setDescription("Studio cao cấp full nội thất, giờ giấc tự do, an ninh 24/7, có hồ bơi và phòng gym. Thích hợp cho người đi làm hoặc sinh viên cần yên tĩnh.");
        p2.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p2.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.ELEVATOR,
                UtilityCodes.SECURITY,
                UtilityCodes.WASHER,
                UtilityCodes.WIFI
        ));
        p2.setStatus(Constants.POST_AVAILABLE);
        p2.setCreatedAt(now - 2 * DAY);
        p2.setUpdatedAt(now - 2 * DAY);
        list.add(p2);

        Post p3 = new Post();
        p3.setPostId("post_003");
        p3.setUserId("user_003");
        p3.setFamilyName("Lê Văn C");
        p3.setTitle("Phòng ghép sinh viên gần ĐH KHXH&NV / KHTN");
        p3.setAddress("227 Nguyễn Văn Cừ, Phường Chợ Quán, TP.HCM");
        p3.setStreetName("Nguyễn Văn Cừ");
        p3.setHouseNumber("227");
        p3.setAddressFingerprint(AddressFingerprintUtil.build(79, 27301, "nguyen van cu", "227"));
        p3.setAddressVerificationStatus(Constants.ADDR_UNVERIFIED);
        p3.setCityCode(79);
        p3.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p3, "Phường Chợ Quán");
        p3.setDescription("Tìm bạn nam ở ghép sạch sẽ, văn minh. Phòng rộng rãi có ban công, gần bến xe buýt và các trường ĐH trung tâm.");
        p3.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80"
        ));
        p3.setUtilities(Arrays.asList(
                UtilityCodes.WIFI,
                UtilityCodes.PARKING,
                UtilityCodes.WASHER
        ));
        p3.setStatus(Constants.POST_AVAILABLE);
        p3.setCreatedAt(now - 3 * DAY);
        p3.setUpdatedAt(now - 3 * DAY);
        list.add(p3);

        Post p4 = new Post();
        p4.setPostId("post_004");
        p4.setUserId("user_004");
        p4.setFamilyName("Phạm Minh D");
        p4.setTitle("Nhà nguyên căn Linh Trung Thủ Đức gần ĐHQG");
        p4.setAddress("12 Linh Trung, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        p4.setStreetName("Linh Trung");
        p4.setHouseNumber("12");
        p4.setAddressFingerprint(AddressFingerprintUtil.build(79, 26800, "linh trung", "12"));
        p4.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p4.setCityCode(79);
        p4.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p4, "Phường Linh Xuân");
        p4.setDescription("Nhà 1 trệt 1 lầu, 2 phòng ngủ, 2 WC, bếp rộng rãi. Thích hợp cho nhóm 4-6 sinh viên ĐHQG ở chung. Hẻm xe hơi ra vào thoải mái.");
        p4.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80"
        ));
        p4.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.FRIDGE,
                UtilityCodes.PARKING,
                UtilityCodes.WASHER
        ));
        p4.setStatus(Constants.POST_EXCHANGED);
        p4.setCreatedAt(now - 5 * DAY);
        p4.setUpdatedAt(now - 4 * DAY);
        list.add(p4);

        Post p5 = new Post();
        p5.setPostId("post_005");
        p5.setUserId("user_004");
        p5.setFamilyName("Phạm Minh D");
        p5.setTitle("Căn nhà cao cấp Linh Xuân gần Làng Đại Học");
        p5.setAddress("88 Linh Xuân, Phường Linh Xuân, TP. Thủ Đức, TP.HCM");
        p5.setStreetName("Linh Xuân");
        p5.setHouseNumber("88");
        p5.setAddressFingerprint(AddressFingerprintUtil.build(79, 26800, "linh xuan", "88"));
        p5.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p5.setCityCode(79);
        p5.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p5, "Phường Linh Xuân");
        p5.setDescription("Phòng mới xây gần ĐHQG, có gác lửng cao không đụng đầu, thiết kế hiện đại, yên tĩnh tuyệt đối để học tập.");
        p5.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
        ));
        p5.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.WIFI,
                UtilityCodes.SECURITY
        ));
        p5.setStatus(Constants.POST_AVAILABLE);
        p5.setCreatedAt(now - 6 * DAY);
        p5.setUpdatedAt(now - 6 * DAY);
        list.add(p5);

        Post p6 = new Post();
        p6.setPostId("post_006");
        p6.setUserId("user_005");
        p6.setFamilyName("Hoàng Thị E");
        p6.setTitle("Căn hộ dịch vụ gần HUTECH / UEF");
        p6.setAddress("482 Điện Biên Phủ, Phường Bình Thạnh, TP.HCM");
        p6.setStreetName("Điện Biên Phủ");
        p6.setHouseNumber("482");
        p6.setAddressFingerprint(AddressFingerprintUtil.build(79, 26929, "dien bien phu", "482"));
        p6.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p6.setCityCode(79);
        p6.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p6, "Phường Bình Thạnh");
        p6.setDescription("Căn hộ dịch vụ mới 100%, có ban công đón nắng, đầy đủ sofa, tủ lạnh, bếp từ. Đi bộ 5 phút tới HUTECH và UEF.");
        p6.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
        ));
        p6.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.FRIDGE,
                UtilityCodes.ELEVATOR,
                UtilityCodes.WIFI
        ));
        p6.setStatus(Constants.POST_AVAILABLE);
        p6.setCreatedAt(now - 12 * HOUR);
        p6.setUpdatedAt(now - 12 * HOUR);
        list.add(p6);

        Post p7 = new Post();
        p7.setPostId("post_007");
        p7.setUserId("user_001");
        p7.setFamilyName("Nguyễn Văn A");
        p7.setTitle("Căn nhà gần Sân Bay & ĐH Mở");
        p7.setAddress("154 Trường Chinh, Phường Tân Sơn Nhất, TP.HCM");
        p7.setStreetName("Trường Chinh");
        p7.setHouseNumber("154");
        p7.setAddressFingerprint(AddressFingerprintUtil.build(79, 26968, "truong chinh", "154"));
        p7.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p7.setCityCode(79);
        p7.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p7, "Phường Tân Sơn Nhất");
        p7.setDescription("Căn nhà khép kín, an ninh tốt, gần chợ và trạm xe buýt. Không gian sạch sẽ, nước nóng năng lượng mặt trời.");
        p7.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80"
        ));
        p7.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.WIFI,
                UtilityCodes.PARKING
        ));
        p7.setStatus(Constants.POST_AVAILABLE);
        p7.setCreatedAt(now - 4 * HOUR);
        p7.setUpdatedAt(now - 4 * HOUR);
        list.add(p7);

        Post p8 = new Post();
        p8.setPostId("post_008");
        p8.setUserId("user_002");
        p8.setFamilyName("Trần Thị B");
        p8.setTitle("Chung cư mini gần ĐH Tôn Đức Thắng / RMIT");
        p8.setAddress("85 Nguyễn Hữu Thọ, Phường Tân Hưng, TP.HCM");
        p8.setStreetName("Nguyễn Hữu Thọ");
        p8.setHouseNumber("85");
        p8.setAddressFingerprint(AddressFingerprintUtil.build(79, 27475, "nguyen huu tho", "85"));
        p8.setAddressVerificationStatus(Constants.ADDR_VERIFIED);
        p8.setCityCode(79);
        p8.setCityName("TP. Hồ Chí Minh");
        applyWardToPost(ctx, p8, "Phường Tân Hưng");
        p8.setDescription("Căn hộ chung cư mini nội thất gỗ sang trọng, có quản lý tòa nhà, giờ giấc tự do 24/7. Rất tiện di chuyển sang TĐT và RMIT.");
        p8.setImages(Arrays.asList(
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80"
        ));
        p8.setUtilities(Arrays.asList(
                UtilityCodes.AIR_CONDITIONER,
                UtilityCodes.ELEVATOR,
                UtilityCodes.SECURITY,
                UtilityCodes.PARKING,
                UtilityCodes.WASHER
        ));
        p8.setStatus(Constants.POST_AVAILABLE);
        p8.setCreatedAt(now - 8 * HOUR);
        p8.setUpdatedAt(now - 8 * HOUR);
        list.add(p8);

        return list;
    }

    // ===================================================================
    // UTIL
    // ===================================================================
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
}