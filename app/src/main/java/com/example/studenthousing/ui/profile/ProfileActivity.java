package com.example.studenthousing.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.databinding.ActivityProfileBinding;
import com.example.studenthousing.ui.auth.LoginActivity;
import com.example.studenthousing.ui.chat.ChatListActivity;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.ui.post.MyPostsActivity;
import com.example.studenthousing.ui.post.SavedHousesActivity;
import com.example.studenthousing.ui.review.UserReviewsActivity;
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

        bind(R.id.rowMyPosts, MyPostsActivity.class);
        bind(R.id.rowSaved, SavedHousesActivity.class);
        bind(R.id.rowSettings, SettingsActivity.class);

        View rowReviewsMe = findViewById(R.id.rowReviewsAboutMe);
        if (rowReviewsMe != null) {
            rowReviewsMe.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, UserReviewsActivity.class);
                intent.putExtra(UserReviewsActivity.EXTRA_TARGET_USER_ID, session.getUserId());
                TextView name = findViewById(R.id.tvProfileName);
                intent.putExtra(UserReviewsActivity.EXTRA_TARGET_USER_NAME, name != null ? name.getText().toString() : "");
                startActivity(intent);
            });
        }

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
        String familyName = user == null || user.getFamilyName() == null ? "Thành viên" : user.getFamilyName();
        String avatarUrl = user == null ? null : user.getAvatarUrl();

        String uni = user != null && user.getUniversityName() != null ? user.getUniversityName() : "Đại học Quốc gia TP.HCM";

        boolean identityVerified = user != null && (user.isIdentityVerified() || "APPROVED".equalsIgnoreCase(data.kycStatus));
        boolean residenceVerified = user != null && (user.isResidenceVerified() || "APPROVED".equalsIgnoreCase(data.residenceStatus));
        boolean anyVerified = identityVerified || residenceVerified;

        // Bind profile header elements directly
        TextView tvName = findViewById(R.id.tvProfileName);
        if (tvName != null) tvName.setText(familyName);

        TextView tvSub = findViewById(R.id.tvProfileSub);
        if (tvSub != null) {
            tvSub.setText(uni);
            tvSub.setVisibility(uni.isEmpty() ? View.GONE : View.VISIBLE);
        }

        ImageView ivAvatarImg = findViewById(R.id.ivProfileAvatarImg);
        TextView tvAvatar = findViewById(R.id.tvProfileAvatar);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (ivAvatarImg != null) {
                ivAvatarImg.setVisibility(View.VISIBLE);
                Glide.with(this).load(avatarUrl).circleCrop().into(ivAvatarImg);
            }
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
        } else {
            if (ivAvatarImg != null) ivAvatarImg.setVisibility(View.GONE);
            if (tvAvatar != null) {
                tvAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setText(initialOf(familyName));
            }
        }

        ImageView badge = findViewById(R.id.tvProfileVerifiedBadge);
        if (badge != null) badge.setVisibility(anyVerified ? View.VISIBLE : View.GONE);

        TextView tvStatPosts = findViewById(R.id.tvStatPosts);
        if (tvStatPosts != null) tvStatPosts.setText(String.valueOf(data.postCount));

        TextView tvStatReviews = findViewById(R.id.tvStatReviews);
        if (tvStatReviews != null) tvStatReviews.setText(String.valueOf(data.reviewCount));

        // Verification buttons & status
        View btnKyc = findViewById(R.id.btnKyc);
        View btnKycAction = findViewById(R.id.btnKycAction);
        View ivKycVerified = findViewById(R.id.ivKycVerified);
        if (identityVerified) {
            if (btnKycAction != null) btnKycAction.setVisibility(View.GONE);
            if (ivKycVerified != null) ivKycVerified.setVisibility(View.VISIBLE);
            if (btnKyc != null) btnKyc.setOnClickListener(v -> Toast.makeText(this, "Danh tính đã được xác thực", Toast.LENGTH_SHORT).show());
        } else {
            if (btnKycAction != null) {
                btnKycAction.setVisibility(View.VISIBLE);
                btnKycAction.setOnClickListener(v -> startActivity(new Intent(this, KycActivity.class)));
            }
            if (ivKycVerified != null) ivKycVerified.setVisibility(View.GONE);
            if (btnKyc != null) btnKyc.setOnClickListener(v -> startActivity(new Intent(this, KycActivity.class)));
        }

        View btnResidence = findViewById(R.id.btnResidenceAuth);
        View btnResidenceAction = findViewById(R.id.btnResidenceAction);
        View ivResidenceVerified = findViewById(R.id.ivResidenceVerified);
        if (residenceVerified) {
            if (btnResidenceAction != null) btnResidenceAction.setVisibility(View.GONE);
            if (ivResidenceVerified != null) ivResidenceVerified.setVisibility(View.VISIBLE);
            if (btnResidence != null) btnResidence.setOnClickListener(v -> Toast.makeText(this, "Cư trú đã được xác thực", Toast.LENGTH_SHORT).show());
        } else {
            if (btnResidenceAction != null) {
                btnResidenceAction.setVisibility(View.VISIBLE);
                btnResidenceAction.setOnClickListener(v -> startActivity(new Intent(this, ResidenceVerificationActivity.class)));
            }
            if (ivResidenceVerified != null) ivResidenceVerified.setVisibility(View.GONE);
            if (btnResidence != null) btnResidence.setOnClickListener(v -> startActivity(new Intent(this, ResidenceVerificationActivity.class)));
        }
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

    private static String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        String last = words[words.length - 1];
        return last.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
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
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        com.google.android.material.badge.BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_messages);
        String userId = session.getUserId();
        new com.example.studenthousing.data.repository.ChatRepository(this).getUserRooms(userId,
                new com.example.studenthousing.util.AppCallback<>() {
                    @Override public void onSuccess(java.util.List<com.example.studenthousing.data.model.ChatRoom> rooms) {
                        runOnUiThread(() -> {
                            int count = 0;
                            if (rooms != null) for (com.example.studenthousing.data.model.ChatRoom room : rooms) {
                                if (room != null && room.getLastMessageTime() > 0 && room.getLastSenderId() != null
                                        && !userId.equals(room.getLastSenderId())) count++;
                            }
                            badge.setVisible(count > 0);
                            if (count > 0) badge.setNumber(count);
                        });
                    }
                    @Override public void onError(Exception error) { runOnUiThread(() -> badge.setVisible(false)); }
                });
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
                } else if (itemId == R.id.nav_messages) {
                    navigateTo(ChatListActivity.class);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                return false;
            });
        }
    }
}
