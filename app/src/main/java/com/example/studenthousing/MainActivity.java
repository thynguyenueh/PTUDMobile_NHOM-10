package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SessionManager session;
    private ReviewRepository reviewRepository;
    private TextView tvRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        com.example.studenthousing.data.local.DatabaseSeeder.seedIfEmpty(this);
        new com.example.studenthousing.util.SyncManager(this).startAutoSync();

        setContentView(R.layout.activity_main);
        reviewRepository = new ReviewRepository(this);

        tvRating = findViewById(R.id.tvRating);

        // Nút Cài đặt (⚙) ở góc trên bên phải -> Mở SettingsActivity
        bind(R.id.btnSettings, SettingsActivity.class);

        // Tương tác phần Thống kê -> Mở Activity tương ứng
        bind(R.id.btnStatRating, UserReviewsActivity.class);
        bind(R.id.btnStatExchanges, ExchangeHistoryActivity.class);
        bind(R.id.btnStatPosts, MyPostsActivity.class);

        // Menu chính
        bind(R.id.btnKyc, KycActivity.class);
        bind(R.id.btnResidenceAuth, ResidenceVerificationActivity.class);

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session != null && session.isLoggedIn()) {
            loadAverageRating();
        }
    }

    /** Cập nhật điểm đánh giá trung bình mỗi khi quay lại màn hình. */
    private void loadAverageRating() {
        if (tvRating == null) return;
        reviewRepository.getAverageRating(session.getUserId(), new AppCallback<Double>() {
            @Override
            public void onSuccess(Double avg) {
                if (avg == null || avg <= 0) return;
                runOnUiThread(() -> tvRating.setText(String.format(Locale.US, "%.1f", avg)));
            }

            @Override
            public void onError(Exception e) { /* giữ nguyên giá trị đang hiển thị */ }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bind(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x -> startActivity(new Intent(MainActivity.this, target)));
        }
    }

    private void setupBottomNav() {
        bind(R.id.btnNavHome, HomeActivity.class);
        bind(R.id.btnNavSaved, SavedHousesActivity.class);
        bind(R.id.btnNavMessages, ChatActivity.class);

        View btnNavProfile = findViewById(R.id.btnNavProfile);
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v ->
                    Toast.makeText(this, "Bạn đang ở màn hình Hồ sơ", Toast.LENGTH_SHORT).show());
        }
    }
}
