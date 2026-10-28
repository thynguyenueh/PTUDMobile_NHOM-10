package com.example.studenthousing.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SearchResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Không dùng layout riêng: chuyển về SearchActivity chính để hiển thị kết quả
        startActivity(new Intent(this, SearchFilterActivity.class));
        finish();
    }
}
