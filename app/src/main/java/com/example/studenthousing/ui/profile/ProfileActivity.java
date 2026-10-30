package com.example.studenthousing.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.studenthousing.R;
import com.example.studenthousing.databinding.ActivityProfileBinding;
import com.example.studenthousing.ui.auth.LoginActivity;

import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.ui.post.MyPostsActivity;
import com.example.studenthousing.ui.post.SavedHousesActivity;

import com.example.studenthousing.ui.verify.KycActivity;
import com.example.studenthousing.ui.verify.ResidenceVerificationActivity;
import com.example.studenthousing.ui.viewmodel.ProfileViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private ProfileViewModel viewModel;
    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        com.example.studenthousing.util.SyncManager.getInstance(getApplicationContext()).startAutoSync();

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfileState().observe(this, this::renderProfile);

        bind(R.id.btnSettings, SettingsActivity.class);

        bind(R.id.btnStatPosts, MyPostsActivity.class);

        bind(R.id.btnKyc, KycActivity.class);
        bind(R.id.btnResidenceAuth, ResidenceVerificationActivity.class);

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session != null && session.isLoggedIn() && viewModel != null) {
            viewModel.loadProfile(session.getUserId());
            updateMessagesBadge();
        }
    }

    private void renderProfile(UiState<ProfileViewModel.ProfileData> state) {
        if (state == null || state.getContent() == null) return;
        ProfileViewModel.ProfileData data = state.getContent();
        com.example.studenthousing.data.model.User user = data.user;
        String sessionName = session.getUserFamilyName();
        String familyName = (user != null && user.getFamilyName() != null && !user.getFamilyName().isEmpty())
                ? user.getFamilyName()
                : (sessionName != null && !sessionName.isEmpty() ? sessionName : "Người dùng");
        String avatarUrl = user == null ? null : user.getAvatarUrl();



        String uni = user != null && user.getUniversityName() != null && !user.getUniversityName().trim().isEmpty()
                ? user.getUniversityName()
                : "Chưa cập nhật";
        bindProfileHeader(binding.getRoot(), familyName, uni, avatarUrl,
                user != null && user.isResidenceVerified(), data.postCount, data.reviewCount, data.exchangeCount);

        String userId = user != null ? user.getUserId() : session.getUserId();
        String savedResidenceStatus = getSharedPreferences(ResidenceVerificationActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(ResidenceVerificationActivity.KEY_STATUS + userId, null);
        String savedKycStatus = getSharedPreferences(KycActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(KycActivity.KEY_STATUS + userId, null);

        boolean residenceVerified = (user != null && user.isResidenceVerified()) || "APPROVED".equalsIgnoreCase(savedResidenceStatus);
        TextView residenceSub = binding.tvResidenceAuthSub;
        if (residenceSub != null) {
            if (residenceVerified) setStatus(residenceSub, "✓ Đã xác thực cư trú", "#719B28");
            else if ("PENDING".equalsIgnoreCase(data.residenceStatus) || "PENDING".equalsIgnoreCase(savedResidenceStatus)) setStatus(residenceSub, "⏳ Đang chờ xác minh", "#EAB308");
            else setStatus(residenceSub, "Chưa xác minh", "#6B7280");
        }

        TextView kycSub = binding.tvKycStatusSub;
        if (kycSub != null) {
            boolean kycVerified = (user != null && user.isVerified()) || "APPROVED".equalsIgnoreCase(savedKycStatus);
            if (kycVerified) setStatus(kycSub, "✓ Đã xác minh", "#719B28");
            else if ("PENDING".equalsIgnoreCase(data.kycStatus) || "PENDING".equalsIgnoreCase(savedKycStatus)) setStatus(kycSub, "⏳ Đang chờ xác minh", "#EAB308");
            else setStatus(kycSub, "Chưa xác minh", "#6B7280");
        }

        // Setup Admin long click approval
        binding.btnKyc.setOnLongClickListener(v -> {
            showAdminApprovalDialog(userId, "KYC / Căn cước công dân", true);
            return true;
        });
        binding.btnResidenceAuth.setOnLongClickListener(v -> {
            showAdminApprovalDialog(userId, "Xác thực cư trú", false);
            return true;
        });
    }

    private void showAdminApprovalDialog(String userId, String title, boolean isKyc) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Admin duyệt hồ sơ")
                .setMessage("Phê duyệt hồ sơ [" + title + "] thành 'Đã xác minh'?")
                .setPositiveButton("Duyệt hồ sơ", (dialog, which) -> {
                    com.example.studenthousing.data.repository.UserRepository userRepo =
                            new com.example.studenthousing.data.repository.UserRepository(this);
                    if (isKyc) {
                        getSharedPreferences(KycActivity.PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(KycActivity.KEY_STATUS + userId, "APPROVED")
                                .apply();
                        userRepo.markIdentityVerified(userId, com.example.studenthousing.util.Constants.IDENTITY_CITIZEN, null);
                    } else {
                        getSharedPreferences(ResidenceVerificationActivity.PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(ResidenceVerificationActivity.KEY_STATUS + userId, "APPROVED")
                                .apply();
                        userRepo.markResidenceVerified(userId, null);
                    }
                    android.widget.Toast.makeText(this, "Đã duyệt thành công! Trạng thái: Đã xác minh", android.widget.Toast.LENGTH_SHORT).show();
                    if (viewModel != null) viewModel.loadProfile(userId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setStatus(TextView view, String text, String color) {
        view.setText(text);
        view.setTextColor(android.graphics.Color.parseColor(color));
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
            v.setOnClickListener(x -> startActivity(new Intent(ProfileActivity.this, target)));
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        if (targetActivity == HomeActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        android.app.ActivityOptions options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0);
        startActivity(intent, options.toBundle());
        finish();
    }

    private void updateMessagesBadge() {
        // Chat replaced with Manage Posts
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    navigateTo(HomeActivity.class);
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    navigateTo(SavedHousesActivity.class);
                    return true;
                } else if (itemId == R.id.nav_manage_posts) {
                    navigateTo(MyPostsActivity.class);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                return false;
            });
        }
    }

    private void bindProfileHeader(View root, String name, String subtitle, String avatarUrl, boolean isVerified,
                                   Integer posts, Integer reviews, Integer exchanges) {
        android.widget.ImageView ivAvatarImg = root.findViewById(R.id.ivProfileAvatarImg);
        TextView tvAvatar = root.findViewById(R.id.tvProfileAvatar);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (ivAvatarImg != null) {
                ivAvatarImg.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(root.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .into(ivAvatarImg);
            }
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
        } else {
            if (ivAvatarImg != null) ivAvatarImg.setVisibility(View.GONE);
            if (tvAvatar != null) {
                tvAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setText(initialOf(name));
            }
        }

        View badge = root.findViewById(R.id.tvProfileVerifiedBadge);
        if (badge != null) {
            badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        ((TextView) root.findViewById(R.id.tvProfileName))
                .setText(name != null && !name.trim().isEmpty() ? name : "Người dùng");

        TextView sub = root.findViewById(R.id.tvProfileSub);
        sub.setText(subtitle != null ? subtitle : "");
        sub.setVisibility(subtitle == null || subtitle.trim().isEmpty() ? View.GONE : View.VISIBLE);

        setStat(root, R.id.tvStatPosts, posts);
        setStat(root, R.id.tvStatReviews, reviews);
        setStat(root, R.id.tvStatExchanges, exchanges);
    }

    private void setStat(View root, int id, Integer value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value == null ? "–" : String.valueOf(value));
    }

    private String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        String last = words[words.length - 1];
        return last.substring(0, 1).toUpperCase(new java.util.Locale("vi"));
    }
}
