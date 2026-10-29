package com.example.studenthousing.ui.partner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.adapter.PostAdapter;
import com.example.studenthousing.ui.chat.ChatActivity;
import com.example.studenthousing.ui.post.DetailActivity;
import com.example.studenthousing.util.AppCallback;

import java.util.ArrayList;
import java.util.List;

public class PublicHostProfileActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    private PostAdapter postAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_host_profile);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            targetUserId = getIntent().getStringExtra("target_user_id");
        }
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            Toast.makeText(this, "Không có thông tin đối tác", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String finalTargetUserId = targetUserId;

        RecyclerView rvHostPosts = findViewById(R.id.rvHostPosts);
        if (rvHostPosts != null) {
            rvHostPosts.setLayoutManager(new LinearLayoutManager(this));
            postAdapter = new PostAdapter(new ArrayList<>(), post -> {
                Intent intent = new Intent(PublicHostProfileActivity.this, DetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                intent.putExtra("POST_ID", post.getPostId());
                startActivity(intent);
            });
            rvHostPosts.setAdapter(postAdapter);
        }

        loadHostProfile(finalTargetUserId);
        loadHostPosts(finalTargetUserId);

        View btnChatWithHost = findViewById(R.id.btnChatWithHost);
        if (btnChatWithHost != null) {
            btnChatWithHost.setOnClickListener(v -> {
                Intent intent = new Intent(PublicHostProfileActivity.this, ChatActivity.class);
                intent.putExtra("target_user_id", finalTargetUserId);
                startActivity(intent);
            });
        }
    }

    private void loadHostProfile(String userId) {
        TextView tvHostFullName = findViewById(R.id.tvHostFullName);
        View ivVerifiedBadge = findViewById(R.id.ivPublicHostVerified);
        ImageView ivHostAvatarImg = findViewById(R.id.ivHostAvatarImg);
        TextView tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        TextView tvStudentInfo = findViewById(R.id.tvStudentInfo);

        TextView tvStudentVerifyStatus = findViewById(R.id.tvStudentVerifyStatus);
        TextView tvCccdVerifyStatus = findViewById(R.id.tvCccdVerifyStatus);

        User cached = new UserDao(this).getById(userId);
        if (cached != null) {
            bindUser(cached, tvHostFullName, ivHostAvatarImg, tvAvatarInitial, ivVerifiedBadge, tvStudentInfo, tvStudentVerifyStatus, tvCccdVerifyStatus);
        }

        new UserRepository(this).getUserProfile(userId, new AppCallback<User>() {
            @Override public void onSuccess(User user) {
                runOnUiThread(() -> bindUser(user, tvHostFullName, ivHostAvatarImg, tvAvatarInitial, ivVerifiedBadge, tvStudentInfo, tvStudentVerifyStatus, tvCccdVerifyStatus));
            }
            @Override public void onError(Exception e) {
                if (cached == null) {
                    runOnUiThread(() -> {
                        if (tvHostFullName != null) tvHostFullName.setText("Không tải được đối tác");
                        if (tvStudentInfo != null) tvStudentInfo.setText("Kiểm tra kết nối mạng và thử lại");
                    });
                }
            }
        });
    }

    private void bindUser(User user, TextView tvHostFullName, ImageView ivHostAvatarImg, TextView tvAvatarInitial, View ivVerifiedBadge, TextView tvStudentInfo,
                          TextView tvStudentVerifyStatus, TextView tvCccdVerifyStatus) {
        if (user == null) return;
        String name = user.getFamilyName() != null && !user.getFamilyName().isEmpty()
                ? user.getFamilyName() : "Đối tác";
        if (tvHostFullName != null) tvHostFullName.setText(name);

        if (ivHostAvatarImg != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ivHostAvatarImg.setVisibility(View.VISIBLE);
            Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(ivHostAvatarImg);
            if (tvAvatarInitial != null) tvAvatarInitial.setVisibility(View.GONE);
        } else {
            if (tvAvatarInitial != null && !name.isEmpty()) {
                tvAvatarInitial.setVisibility(View.VISIBLE);
                tvAvatarInitial.setText(name.substring(0, 1).toUpperCase());
            }
            if (ivHostAvatarImg != null) ivHostAvatarImg.setVisibility(View.GONE);
        }

        boolean isStudentVerified = user.isIdentityVerified() || (user.getIdentityType() != null && "STUDENT".equalsIgnoreCase(user.getIdentityType()));
        boolean isCccdVerified = user.isIdentityVerified() || (user.getIdentityType() != null && "CITIZEN".equalsIgnoreCase(user.getIdentityType()));
        boolean isPoliceVerified = user.isResidenceVerified();

        View rowStudent = findViewById(R.id.layoutStudentVerifyRow);
        View rowCccd = findViewById(R.id.layoutCccdVerifyRow);

        if (rowStudent != null) rowStudent.setVisibility(isStudentVerified ? View.VISIBLE : View.GONE);
        if (rowCccd != null) rowCccd.setVisibility(isCccdVerified ? View.VISIBLE : View.GONE);

        if (ivVerifiedBadge != null) {
            ivVerifiedBadge.setVisibility(isStudentVerified || isCccdVerified || isPoliceVerified ? View.VISIBLE : View.GONE);
        }
        if (tvStudentInfo != null) {
            String role = "Thành viên";
            String uni = user.getUniversityName() != null && !user.getUniversityName().isEmpty()
                    ? " · " + user.getUniversityName() : "";
            tvStudentInfo.setText(role + uni);
        }

        if (tvStudentVerifyStatus != null) {
            tvStudentVerifyStatus.setText("Đã xác thực");
            tvStudentVerifyStatus.setTextColor(getColor(R.color.primary));
        }
        if (tvCccdVerifyStatus != null) {
            tvCccdVerifyStatus.setText("Đã xác thực");
            tvCccdVerifyStatus.setTextColor(getColor(R.color.primary));
        }
    }

    private void loadHostPosts(String userId) {
        new PostRepository(this).getPostsByUserId(userId, new AppCallback<>() {
            @Override public void onSuccess(List<AccommodationPost> posts) {
                runOnUiThread(() -> {
                    if (postAdapter != null) {
                        postAdapter.updateData(posts);
                    }
                });
            }
            @Override public void onError(Exception e) {}
        });
    }
}