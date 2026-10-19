package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        bind(R.id.btnKycVerification, KycActivity.class);
        bind(R.id.btnNotificationSettings, "Cài đặt thông báo");
        bind(R.id.btnLanguageSettings, "Ngôn ngữ");
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
        android.widget.TextView tvSettingName = findViewById(R.id.tvSettingName);
        android.widget.TextView tvSettingPhone = findViewById(R.id.tvSettingPhone);
        android.widget.TextView tvSettingAvatar = findViewById(R.id.tvSettingAvatar);

        String familyName = session.getUserFamilyName();
        String phone = session.getUserPhone();

        if (tvSettingName != null && familyName != null && !familyName.isEmpty()) {
            tvSettingName.setText(familyName);
            if (tvSettingAvatar != null) {
                tvSettingAvatar.setText(familyName.substring(0, 1).toUpperCase());
            }
        }

        if (tvSettingPhone != null && phone != null && !phone.isEmpty()) {
            String maskedPhone = phone.length() >= 4 ? phone.substring(0, Math.min(phone.length(), 3)) + " ••• ••" + phone.substring(phone.length() - 2) : phone;
            tvSettingPhone.setText(maskedPhone + " · Tài khoản Đã xác minh");
        }
    }

    private void bind(int viewId, String toastMsg) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x -> Toast.makeText(SettingsActivity.this, toastMsg, Toast.LENGTH_SHORT).show());
        }
    }

    private void bind(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x -> startActivity(new Intent(SettingsActivity.this, target)));
        }
    }
}
