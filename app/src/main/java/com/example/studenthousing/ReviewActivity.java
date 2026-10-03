package com.example.studenthousing;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReviewActivity extends AppCompatActivity {

    private TextView tvStars;
    private int currentRating = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        tvStars = findViewById(R.id.tvStars);
        if (tvStars != null) {
            tvStars.setOnClickListener(v -> toggleStars());
        }

        if (findViewById(R.id.btnUploadImage) != null) {
            findViewById(R.id.btnUploadImage).setOnClickListener(v ->
                    Toast.makeText(this, "Chọn ảnh đính kèm", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnSubmit) != null) {
            findViewById(R.id.btnSubmit).setOnClickListener(v -> {
                Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá (" + currentRating + " sao)!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    private void toggleStars() {
        currentRating = (currentRating % 5) + 1;
        StringBuilder stars = new StringBuilder();
        for (int index = 0; index < 5; index++) {
            stars.append(index < currentRating ? "★  " : "☆  ");
        }
        tvStars.setText(stars.toString().trim());
        Toast.makeText(this, "Đánh giá: " + currentRating + " sao", Toast.LENGTH_SHORT).show();
    }
}
