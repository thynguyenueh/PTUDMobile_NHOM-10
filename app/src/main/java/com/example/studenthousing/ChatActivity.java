package com.example.studenthousing;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        if (findViewById(R.id.btnCall) != null) {
            findViewById(R.id.btnCall).setOnClickListener(v ->
                    Toast.makeText(this, "Gọi điện cho đối tác", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnSend) != null) {
            findViewById(R.id.btnSend).setOnClickListener(v ->
                    Toast.makeText(this, "Đã gửi tin nhắn!", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnCardAction) != null) {
            findViewById(R.id.btnCardAction).setOnClickListener(v ->
                    Toast.makeText(this, "Đã xác nhận thỏa thuận!", Toast.LENGTH_SHORT).show());
        }
    }
}
