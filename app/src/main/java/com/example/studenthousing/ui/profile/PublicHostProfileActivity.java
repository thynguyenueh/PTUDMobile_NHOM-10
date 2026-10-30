package com.example.studenthousing.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.adapter.PostAdapter;
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
            android.widget.Toast.makeText(this, "Không có thông tin đối tác", android.widget.Toast.LENGTH_SHORT).show();
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
            btnChatWithHost.setOnClickListener(v -> openSmsApp(finalTargetUserId, "Xin chào, tôi muốn trao đổi về nhà ở."));
        }
    }

    private void openSmsApp(String targetUserId, String defaultMessage) {
        UserDao userDao = new UserDao(this);
        User host = targetUserId != null ? userDao.getById(targetUserId) : null;
        String phone = host != null && host.getPhone() != null && !host.getPhone().isEmpty()
                ? host.getPhone() : "0988123456";
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + phone.trim()));
        smsIntent.putExtra("sms_body", defaultMessage);
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:" + phone.trim()));
                viewIntent.putExtra("sms_body", defaultMessage);
                startActivity(viewIntent);
            } catch (Exception ex) {
                android.widget.Toast.makeText(this, "Không tìm thấy ứng dụng nhắn tin", android.widget.Toast.LENGTH_SHORT).show();
            }
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
        TextView tvPoliceVerifyStatus = findViewById(R.id.tvPoliceVerifyStatus);

        User cached = new UserDao(this).getById(userId);
        if (cached != null) {
            bindUser(cached, tvHostFullName, ivHostAvatarImg, tvAvatarInitial, ivVerifiedBadge, tvStudentInfo, tvStudentVerifyStatus, tvCccdVerifyStatus, tvPoliceVerifyStatus);
        }

        new UserRepository(this).getUserProfile(userId, new AppCallback<User>() {
            @Override public void onSuccess(User user) {
                runOnUiThread(() -> bindUser(user, tvHostFullName, ivHostAvatarImg, tvAvatarInitial, ivVerifiedBadge, tvStudentInfo, tvStudentVerifyStatus, tvCccdVerifyStatus, tvPoliceVerifyStatus));
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
                          TextView tvStudentVerifyStatus, TextView tvCccdVerifyStatus, TextView tvPoliceVerifyStatus) {
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

        if (ivVerifiedBadge != null) {
            ivVerifiedBadge.setVisibility(user.isResidenceVerified() || user.isVerified() ? View.VISIBLE : View.GONE);
        }
        if (tvStudentInfo != null) {
            String role = "Thành viên";
            String uni = user.getUniversityName() != null && !user.getUniversityName().isEmpty()
                    ? " · " + user.getUniversityName() : "";
            tvStudentInfo.setText(role + uni);
        }

        boolean isStudentVerified = user.isVerified() || (user.getIdentityType() != null && !"NONE".equalsIgnoreCase(user.getIdentityType()));
        boolean isCccdVerified = user.isVerified() || user.isIdentityVerified();
        boolean isPoliceVerified = user.isResidenceVerified();

        if (tvStudentVerifyStatus != null) {
            tvStudentVerifyStatus.setText(isStudentVerified ? "Đã xác thực" : "Chưa xác thực");
            tvStudentVerifyStatus.setTextColor(getColor(isStudentVerified ? R.color.primary : R.color.text_sub));
        }
        if (tvCccdVerifyStatus != null) {
            tvCccdVerifyStatus.setText(isCccdVerified ? "Đã xác thực" : "Chưa xác thực");
            tvCccdVerifyStatus.setTextColor(getColor(isCccdVerified ? R.color.primary : R.color.text_sub));
        }
        if (tvPoliceVerifyStatus != null) {
            tvPoliceVerifyStatus.setText(isPoliceVerified ? "Đã xác thực" : "Chưa xác thực");
            tvPoliceVerifyStatus.setTextColor(getColor(isPoliceVerified ? R.color.primary : R.color.text_sub));
        }
    }

    private void loadHostPosts(String userId) {
        new PostRepository(this).getPostsByUserId(userId, new AppCallback<>() {
            @Override public void onSuccess(List<Post> posts) {
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
