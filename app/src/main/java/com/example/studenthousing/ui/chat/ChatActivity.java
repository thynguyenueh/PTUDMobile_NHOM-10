package com.example.studenthousing.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.ui.adapter.MessageAdapter;
import com.example.studenthousing.ui.auth.LoginActivity;
import com.example.studenthousing.ui.partner.PublicHostProfileActivity;
import com.example.studenthousing.ui.viewmodel.ChatViewModel;
import com.example.studenthousing.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private EditText etMessageInput;
    private RecyclerView rvMessages;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;

    private String currentUserId;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAndSendImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ============ 1. Verify session (không cho phép null) ============
        currentUserId = SessionManager.getUid(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Bạn cần đăng nhập để chat", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }

        // ============ 2. Verify target user (không fallback "demo_target_user") ============
        String targetUserId = getIntent().getStringExtra("target_user_id");
        if (targetUserId == null) targetUserId = getIntent().getStringExtra("EXTRA_TARGET_USER_ID");
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy người dùng đối tác", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String chatTargetUserId = targetUserId;

        // ============ 3. Init views ============
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        etMessageInput = findViewById(R.id.etMessageInput);
        rvMessages = findViewById(R.id.rvMessages);

        if (etMessageInput != null) {
            etMessageInput.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_MESSAGE_LENGTH)
            });
        }

        String roomName = getIntent().getStringExtra("ROOM_NAME");
        if (roomName == null || roomName.isEmpty())
            roomName = getIntent().getStringExtra("EXTRA_TARGET_USER_NAME");
        if (roomName == null || roomName.isEmpty())
            roomName = getIntent().getStringExtra("POST_TITLE");

        TextView tvHeaderName = findViewById(R.id.tvChatHeaderName);
        TextView tvAvatarLetter = findViewById(R.id.tvChatAvatarLetter);
        if (roomName != null && !roomName.isEmpty()) {
            if (tvHeaderName != null) tvHeaderName.setText(roomName);
            if (tvAvatarLetter != null)
                tvAvatarLetter.setText(roomName.substring(0, 1).toUpperCase());
        }

        String passedRoomId = getIntent().getStringExtra("ROOM_ID");
        String postId = getIntent().getStringExtra("POST_ID");
        if (postId == null) postId = getIntent().getStringExtra("post_id");
        final String finalPostId = postId;

        // ============ 4. TẠO ADAPTER TRƯỚC KHI OBSERVE (fix NPE) ============
        adapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // ============ 5. Observe ============
        viewModel.getTargetUserProfile().observe(this, user -> {
            if (user == null) return;
            if (user.getFamilyName() != null && !user.getFamilyName().isEmpty()) {
                if (tvHeaderName != null) tvHeaderName.setText(user.getFamilyName());
                if (tvAvatarLetter != null)
                    tvAvatarLetter.setText(user.getFamilyName().substring(0, 1).toUpperCase());
            }
        });

        viewModel.getMessagesState().observe(this, state -> {
            if (state == null || state.getContent() == null) return;
            List<Message> messages = state.getContent();
            adapter.updateData(messages);
            if (!messages.isEmpty()) {
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getActionEvent().observe(this, event -> {
            if (event == null) return;
            if ("MESSAGE_SENT".equals(event)) {
                if (etMessageInput != null) etMessageInput.setText("");
            } else if ("UPLOADING_IMAGE".equals(event)) {
                Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
            } else if (event.startsWith("ERROR: ")) {
                Toast.makeText(this, event.substring(7), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.initChat(currentUserId, targetUserId, passedRoomId, null, roomName);

        // ============ 6. Buttons ============
        View btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Mở profile partner khi bấm avatar hoặc tên
        View.OnClickListener openProfile = v -> {
            Intent intent = new Intent(ChatActivity.this, PublicHostProfileActivity.class);
            intent.putExtra(PublicHostProfileActivity.EXTRA_TARGET_USER_ID, chatTargetUserId);
            startActivity(intent);
        };
        if (tvAvatarLetter != null && tvAvatarLetter.getParent() instanceof View) {
            ((View) tvAvatarLetter.getParent()).setOnClickListener(openProfile);
        }
        if (tvHeaderName != null) tvHeaderName.setOnClickListener(openProfile);

        View btnCall = findViewById(R.id.btnCall);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                User targetUser = new UserDao(ChatActivity.this).getById(chatTargetUserId);
                String phone = targetUser != null ? targetUser.getPhone() : null;
                if (phone == null || phone.trim().isEmpty()) {
                    Toast.makeText(ChatActivity.this,
                            "Đối tác chưa cung cấp số điện thoại", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent callIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.fromParts("tel", phone.trim(), null));
                if (callIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(callIntent);
                } else {
                    Toast.makeText(ChatActivity.this,
                            "Không tìm thấy ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnAttach = findViewById(R.id.btnAttach);
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }
    }

    private void sendMessage() {
        if (etMessageInput == null) return;
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.sendMessage(content);
    }

    private void uploadAndSendImage(Uri uri) {
        String caption = (etMessageInput != null)
                ? etMessageInput.getText().toString().trim()
                : "";
        viewModel.uploadAndSendImage(uri, caption);
    }
}