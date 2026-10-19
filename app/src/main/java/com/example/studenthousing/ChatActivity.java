package com.example.studenthousing;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.util.AppCallback;

public class ChatActivity extends AppCompatActivity {

    private EditText etMessageInput;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRepository = new ChatRepository(this);
        etMessageInput = findViewById(R.id.etMessageInput);

        String roomName = getIntent().getStringExtra("ROOM_NAME");
        if (roomName != null && !roomName.isEmpty()) {
            android.widget.TextView tvHeaderName = findViewById(R.id.tvChatHeaderName);
            android.widget.TextView tvAvatarLetter = findViewById(R.id.tvChatAvatarLetter);
            if (tvHeaderName != null) {
                tvHeaderName.setText(roomName);
            }
            if (tvAvatarLetter != null && roomName.length() > 0) {
                tvAvatarLetter.setText(roomName.substring(0, 1).toUpperCase());
            }
        }

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        if (findViewById(R.id.btnCall) != null) {
            findViewById(R.id.btnCall).setOnClickListener(v ->
                    Toast.makeText(this, "Đang gọi điện cho đối tác...", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnSend) != null) {
            findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        }

        if (findViewById(R.id.btnCardAction) != null) {
            findViewById(R.id.btnCardAction).setOnClickListener(v ->
                    Toast.makeText(this, "Đã xác nhận thỏa thuận đổi chỗ ở!", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendMessage() {
        if (etMessageInput == null) return;
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        Message msg = new Message();
        String roomId = "room_sample";
        msg.setRoomId(roomId);
        msg.setSenderId("current_user");
        msg.setContent(content);
        msg.setType("TEXT");
        msg.setSentAt(System.currentTimeMillis());

        chatRepository.sendMessage(roomId, msg, new AppCallback<String>() {
            @Override
            public void onSuccess(String data) {
                runOnUiThread(() -> {
                    etMessageInput.setText("");
                    Toast.makeText(ChatActivity.this, "Đã gửi tin nhắn!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    etMessageInput.setText("");
                    Toast.makeText(ChatActivity.this, "Đã gửi tin nhắn (Lưu cục bộ)!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

