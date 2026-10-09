package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvRating = findViewById(R.id.tvRating);
        if (tvRating != null) {
            tvRating.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ReviewActivity.class);
                startActivity(intent);
            });
        }

        View btnKyc = findViewById(R.id.btnKyc);
        if (btnKyc != null) {
            btnKyc.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, KycActivity.class);
                startActivity(intent);
            });
        }

        View btnResidenceAuth = findViewById(R.id.btnResidenceAuth);
        if (btnResidenceAuth != null) {
            btnResidenceAuth.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ResidenceVerificationActivity.class);
                startActivity(intent);
            });
        }

        View btnMyPosts = findViewById(R.id.btnMyPosts);
        if (btnMyPosts != null) {
            btnMyPosts.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            });
        }

        View btnHistory = findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                    Toast.makeText(this, "Lịch sử đổi nhà (3 lượt đã hoàn tất)", Toast.LENGTH_SHORT).show());
        }

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Setup Bottom Navigation Bar Events
        setupBottomNav();
    }

    private void setupBottomNav() {
        View btnNavHome = findViewById(R.id.btnNavHome);
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            });
        }

        View btnNavSaved = findViewById(R.id.btnNavSaved);
        if (btnNavSaved != null) {
            btnNavSaved.setOnClickListener(v ->
                    Toast.makeText(this, "Danh sách nhà đã lưu", Toast.LENGTH_SHORT).show());
        }

        View btnNavMessages = findViewById(R.id.btnNavMessages);
        if (btnNavMessages != null) {
            btnNavMessages.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        }

        View btnNavProfile = findViewById(R.id.btnNavProfile);
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v ->
                    Toast.makeText(this, "Bạn đang ở màn hình Hồ sơ", Toast.LENGTH_SHORT).show());
        }
    }
}
