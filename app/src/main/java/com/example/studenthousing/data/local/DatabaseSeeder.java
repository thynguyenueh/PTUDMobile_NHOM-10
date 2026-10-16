package com.example.studenthousing.data.local;

import android.content.Context;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.User;

import java.util.Arrays;
import java.util.Collections;

public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {
        PostDao postDao = new PostDao(context);
        ExchangeDao exchangeDao = new ExchangeDao(context);
        ReviewDao reviewDao = new ReviewDao(context);
        UserDao userDao = new UserDao(context);

        // 1. Seed Users if empty
        if (userDao.getById("user_co_mai") == null) {
            User u1 = new User();
            u1.setUserId("user_co_mai");
            u1.setFamilyName("Gia đình Cô Mai");
            u1.setPhone("0901234567");
            u1.setEmail("comai@studenthousing.vn");
            u1.setResidenceVerified(true);
            userDao.upsert(u1);

            User u2 = new User();
            u2.setUserId("user_chu_nam");
            u2.setFamilyName("Gia đình Chú Nam");
            u2.setPhone("0987654321");
            u2.setEmail("chunam@studenthousing.vn");
            u2.setResidenceVerified(true);
            userDao.upsert(u2);

            User u3 = new User();
            u3.setUserId("user_tuan");
            u3.setFamilyName("Bạn Tuấn (ĐH Bách Khoa)");
            u3.setPhone("0911223344");
            u3.setEmail("tuan@studenthousing.vn");
            u3.setResidenceVerified(true);
            userDao.upsert(u3);
        }

        // 2. Seed Posts if empty
        if (postDao.getAll().isEmpty()) {
            AccommodationPost p1 = new AccommodationPost();
            p1.setPostId("post_001");
            p1.setUserId("user_co_mai");
            p1.setFamilyName("Gia đình Cô Mai");
            p1.setTitle("Căn hộ cao cấp Thảo Điền, Quận 2");
            p1.setAddress("Phường Thảo Điền, Quận 2, TP. Hồ Chí Minh");
            p1.setPrice(5.0);
            p1.setArea(65.0);
            p1.setHouseType("Căn hộ");
            p1.setDescription("Căn hộ thoáng mát, đầy đủ tiện nghi, phù hợp đổi nhà học kỳ.");
            p1.setImages(Collections.singletonList("home_living_room"));
            p1.setUtilities(Arrays.asList("Wifi", "Điều hòa", "Nhà bếp", "Chỗ để xe"));
            p1.setPoliceVerified(true);
            p1.setStatus("Đang hiển thị");
            p1.setDistanceKm(1.2);
            postDao.upsert(p1);

            AccommodationPost p2 = new AccommodationPost();
            p2.setPostId("post_002");
            p2.setUserId("user_chu_nam");
            p2.setFamilyName("Gia đình Chú Nam");
            p2.setTitle("Nhà trọ sinh viên gần ĐHQG - Linh Trung");
            p2.setAddress("Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh");
            p2.setPrice(2.5);
            p2.setArea(35.0);
            p2.setHouseType("Nhà trọ");
            p2.setDescription("Gần trạm xe buýt, Làng Đại học, an ninh tốt, giờ giấc tự do.");
            p2.setImages(Collections.singletonList("home_linh_trung"));
            p2.setUtilities(Arrays.asList("Wifi", "Máy lạnh", "Gần KTX"));
            p2.setPoliceVerified(true);
            p2.setStatus("Đang hiển thị");
            p2.setDistanceKm(1.8);
            postDao.upsert(p2);

            AccommodationPost p3 = new AccommodationPost();
            p3.setPostId("post_003");
            p3.setUserId("user_tuan");
            p3.setFamilyName("Bạn Tuấn (ĐH Bách Khoa)");
            p3.setTitle("Phòng trọ giá rẻ Linh Xuân");
            p3.setAddress("Phường Linh Xuân, TP. Thủ Đức, TP. Hồ Chí Minh");
            p3.setPrice(3.2);
            p3.setArea(45.0);
            p3.setHouseType("Phòng trọ");
            p3.setDescription("Phòng rộng rãi sạch sẽ, tiện di chuyển các trường ĐHQG.");
            p3.setImages(Collections.singletonList("house_linh_xuan"));
            p3.setUtilities(Arrays.asList("Wifi", "Chỗ để xe"));
            p3.setPoliceVerified(false);
            p3.setStatus("Đang hiển thị");
            p3.setDistanceKm(3.5);
            postDao.upsert(p3);
        }

        // 3. Seed Exchange Requests if empty
        if (exchangeDao.getBySender("current_user").isEmpty()) {
            ExchangeRequest r1 = new ExchangeRequest();
            r1.setRequestId("req_001");
            r1.setSenderId("current_user");
            r1.setReceiverId("user_co_mai");
            r1.setSenderPostId("post_002");
            r1.setReceiverPostId("post_001");
            r1.setExchangePeriod("Học kỳ 1 (09/2026 - 01/2027)");
            r1.setMessage("Thỏa thuận đổi nhà 2 chiều đã hoàn tất thành công.");
            r1.setStatus("COMPLETED");
            r1.setCreatedAt(System.currentTimeMillis() - 86400000L * 30);
            exchangeDao.upsert(r1);

            ExchangeRequest r2 = new ExchangeRequest();
            r2.setRequestId("req_002");
            r2.setSenderId("current_user");
            r2.setReceiverId("user_chu_nam");
            r2.setSenderPostId("post_001");
            r2.setReceiverPostId("post_002");
            r2.setExchangePeriod("Học kỳ 2 (02/2026 - 06/2026)");
            r2.setMessage("Thỏa thuận đổi nhà cho sinh viên đã hoàn tất.");
            r2.setStatus("COMPLETED");
            r2.setCreatedAt(System.currentTimeMillis() - 86400000L * 60);
            exchangeDao.upsert(r2);

            ExchangeRequest r3 = new ExchangeRequest();
            r3.setRequestId("req_003");
            r3.setSenderId("current_user");
            r3.setReceiverId("user_tuan");
            r3.setSenderPostId("post_001");
            r3.setReceiverPostId("post_003");
            r3.setExchangePeriod("Học kỳ Hè (07/2026 - 08/2026)");
            r3.setMessage("Đổi phòng thời hạn ngắn hoàn tất.");
            r3.setStatus("COMPLETED");
            r3.setCreatedAt(System.currentTimeMillis() - 86400000L * 90);
            exchangeDao.upsert(r3);
        }

        // 4. Seed Reviews if empty
        if (reviewDao.getByReviewee("current_user").isEmpty()) {
            Review rv1 = new Review();
            rv1.setReviewId("rev_001");
            rv1.setRequestId("req_001");
            rv1.setReviewerId("user_co_mai");
            rv1.setReviewerName("Gia đình Cô Mai");
            rv1.setRevieweeId("current_user");
            rv1.setRating(5);
            rv1.setComment("Gia đình rất lịch sự, giữ gìn nhà cửa sạch sẽ chu đáo.\n(Điểm hài lòng: Sạch sẽ, Thân thiện)");
            rv1.setCreatedAt(System.currentTimeMillis() - 86400000L * 5);
            reviewDao.upsert(rv1);

            Review rv2 = new Review();
            rv2.setReviewId("rev_002");
            rv2.setRequestId("req_003");
            rv2.setReviewerId("user_tuan");
            rv2.setReviewerName("Bạn Tuấn (ĐH Bách Khoa)");
            rv2.setRevieweeId("current_user");
            rv2.setRating(5);
            rv2.setComment("Giao tiếp thân thiện, trao đổi nhà đúng thời hạn cam kết.\n(Điểm hài lòng: Đúng giờ, Hỗ trợ tốt)");
            rv2.setCreatedAt(System.currentTimeMillis() - 86400000L * 15);
            reviewDao.upsert(rv2);
        }
    }
}
