package com.example.studenthousing;

import android.widget.TextView;
import android.os.Bundle;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        TextView tvRating = findViewById(R.id.tvRating);

        tvRating.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReviewActivity.class);
            startActivity(intent);
        });
    }
}