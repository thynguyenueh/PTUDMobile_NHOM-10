package com.example.studenthousing.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.ui.auth.LoginActivity;
import com.example.studenthousing.ui.auth.RegisterActivity;
import com.example.studenthousing.ui.verify.KycActivity;
import com.example.studenthousing.util.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        session = new SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, RegisterActivity.class);
                intent.putExtra("IS_EDIT_MODE", true);
                startActivity(intent);
            });
        }
        bind(R.id.btnTerms, "Điều khoản dịch vụ");
        bind(R.id.btnPrivacy, "Chính sách bảo mật");

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.logout();
                Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileInfo();
    }

    private void loadProfileInfo() {
        TextView tvSettingName = findViewById(R.id.tvSettingName);
        TextView tvSettingPhone = findViewById(R.id.tvSettingPhone);
        ImageView ivSettingAvatarImg = findViewById(R.id.ivSettingAvatarImg);
        TextView tvSettingAvatar = findViewById(R.id.tvSettingAvatar);
        TextView tvSettingUniversity = findViewById(R.id.tvSettingUniversity);

        String familyName = session.getUserFamilyName();
        String phone = session.getUserPhone();
        String avatarUrl = session.getAvatarUrl();
        String userId = session.getUserId();

        if (tvSettingName != null && familyName != null && !familyName.isEmpty()) {
            tvSettingName.setText(familyName);
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (ivSettingAvatarImg != null) {
                ivSettingAvatarImg.setVisibility(View.VISIBLE);
                Glide.with(this).load(avatarUrl).circleCrop().into(ivSettingAvatarImg);
            }
            if (tvSettingAvatar != null) tvSettingAvatar.setVisibility(View.GONE);
        } else {
            if (ivSettingAvatarImg != null) ivSettingAvatarImg.setVisibility(View.GONE);
            if (tvSettingAvatar != null) {
                tvSettingAvatar.setVisibility(View.VISIBLE);
                if (familyName != null && !familyName.isEmpty()) {
                    tvSettingAvatar.setText(familyName.substring(0, 1).toUpperCase());
                }
            }
        }

        if (tvSettingPhone != null && phone != null && !phone.isEmpty()) {
            String maskedPhone = phone.length() >= 4 ? phone.substring(0, 3) + " ••• ••" + phone.substring(phone.length() - 2) : phone;
            tvSettingPhone.setText(maskedPhone + " · Tài khoản Đã xác minh");
        }

        if (userId != null) {
            UserDao userDao = new UserDao(this);
            User u = userDao.getById(userId);
            if (u != null) {
                if (tvSettingUniversity != null) {
                    tvSettingUniversity.setText(u.getUniversityName() != null && !u.getUniversityName().isEmpty() ? u.getUniversityName() : (u.getUniversityCode() != null ? u.getUniversityCode() : "Chưa cập nhật"));
                }
            }
        }
    }

    private void bind(int viewId, String toastMsg) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x -> Toast.makeText(SettingsActivity.this, toastMsg, Toast.LENGTH_SHORT).show());
        }
    }
}
