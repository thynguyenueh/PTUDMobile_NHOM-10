package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private ImageView btnTogglePassword;
    private AppCompatButton btnLogin, btnSecondary;
    private TextView tvForgotPassword, tvRegister;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ các View
        initViews();

        // 2. Cài đặt sự kiện Click
        setupEvents();
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSecondary = findViewById(R.id.btnSecondary);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupEvents() {
        // Nút ẩn/hiện mật khẩu
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Nút Đăng nhập
        btnLogin.setOnClickListener(v -> handleLogin());

        // Nút Tùy chọn khác
        btnSecondary.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );

        // Quên mật khẩu
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Chuyển sang Quên mật khẩu", Toast.LENGTH_SHORT).show()
        );

        // CHUYỂN SANG MÀN HÌNH ĐĂNG KÝ HỘ GIA ĐÌNH
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Mật khẩu phải chứa ít nhất 8 ký tự");
            etPassword.requestFocus();
            return;
        }

        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        User user = new User();
        user.setUserId("user_" + phone);
        user.setPhone(phone);
        user.setFamilyName("Gia đình " + suffix);
        user.setRole("PARENT");
        user.setCreatedAt(System.currentTimeMillis());

        UserRepository userRepository = new UserRepository(this);
        userRepository.saveUserProfile(user, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void d) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Đăng nhập & Lưu Firestore thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Đã lưu phiên đăng nhập cục bộ!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}
