package com.example.studenthousing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    public static final String EXTRA_TARGET_USER_NAME = "target_user_name";

    private SessionManager session;
    private ReviewRepository reviewRepository;
    private LinearLayout layoutReviewsContainer;
    private TextView tvAverageRating;
    private TextView tvTotalReviews;
    private View layoutBottomAction;

    private String targetUserId;
    private String targetUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reviews);

        session = new SessionManager(this);
        reviewRepository = new ReviewRepository(this);

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        targetUserName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutReviewsContainer = findViewById(R.id.layoutReviewsContainer);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        layoutBottomAction = findViewById(R.id.layoutBottomAction);

        String currentUserId = session.getUserId() != null ? session.getUserId() : "current_user";
        boolean isOtherUser = targetUserId != null && !targetUserId.equals(currentUserId);

        if (layoutBottomAction != null) {
            layoutBottomAction.setVisibility(isOtherUser ? View.VISIBLE : View.GONE);
        }

        View btnWriteReview = findViewById(R.id.btnWriteReview);
        if (btnWriteReview != null) {
            btnWriteReview.setOnClickListener(v -> {
                Intent intent = new Intent(UserReviewsActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, targetUserId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, targetUserName != null ? targetUserName : "Đối tác đổi nhà");
                startActivity(intent);
            });
        }

        loadReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
    }

    private void loadReviews() {
        String userIdToFetch = targetUserId != null ? targetUserId
                : (session.getUserId() != null ? session.getUserId() : "current_user");

        reviewRepository.getByReviewee(userIdToFetch, new AppCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> displayReviews(reviews));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> displayReviews(null));
            }
        });
    }

    private void displayReviews(List<Review> reviews) {
        if (layoutReviewsContainer == null) return;
        layoutReviewsContainer.removeAllViews();

        if (reviews != null && !reviews.isEmpty()) {
            double total = 0;
            for (Review r : reviews) {
                total += r.getRating();
                addReviewItemView(
                        r.getReviewerName() != null ? r.getReviewerName() : "Đối tác đổi nhà",
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt(),
                        r.getImageUri()
                );
            }
            double avg = total / reviews.size();
            tvAverageRating.setText(String.format(Locale.US, "%.1f", avg));
            tvTotalReviews.setText("Dựa trên " + reviews.size() + " đánh giá từ đối tác");
        } else {
            // Hiển thị danh sách mẫu nếu chưa có dữ liệu từ DB/Firestore
            tvAverageRating.setText("4.9");
            tvTotalReviews.setText("Dựa trên 2 đánh giá từ đối tác đổi nhà");

            addReviewItemView("Gia đình Cô Mai", 5,
                    "Gia đình rất lịch sự, giữ gìn nhà cửa sạch sẽ chu đáo.\n(Điểm hài lòng: Sạch sẽ, Thân thiện)",
                    System.currentTimeMillis() - 86400000L * 5, null);

            addReviewItemView("Bạn Nam - ĐH Bách Khoa", 5,
                    "Giao tiếp thân thiện, trao đổi nhà đúng thời hạn cam kết.\n(Điểm hài lòng: Đúng giờ, Hỗ trợ tốt)",
                    System.currentTimeMillis() - 86400000L * 15, null);
        }
    }

    private void addReviewItemView(String name, int rating, String comment, long timestamp, String imagePath) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_review, layoutReviewsContainer, false);

        TextView tvAvatar = itemView.findViewById(R.id.tvReviewerAvatar);
        TextView tvName = itemView.findViewById(R.id.tvReviewerName);
        TextView tvDate = itemView.findViewById(R.id.tvReviewDate);
        TextView tvStars = itemView.findViewById(R.id.tvReviewRatingStars);
        TextView tvComment = itemView.findViewById(R.id.tvReviewComment);
        ImageView imgAttachment = itemView.findViewById(R.id.imgReviewAttachment);

        if (name != null && !name.isEmpty()) {
            tvName.setText(name);
            tvAvatar.setText(name.substring(0, 1).toUpperCase());
        }

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating ? "★" : "☆");
        }
        tvStars.setText(stars.toString());

        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(timestamp)) + " · Đối tác đổi nhà");
        }

        tvComment.setText(comment != null ? comment : "");

        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                imgAttachment.setVisibility(View.VISIBLE);
                imgAttachment.setImageURI(Uri.fromFile(imgFile));
            }
        }

        layoutReviewsContainer.addView(itemView);
    }
}
