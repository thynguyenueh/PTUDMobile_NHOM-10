package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private View btnLogin, btnGoogle;
    private TextView tvForgotPassword, tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupEvents();
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupEvents() {
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> handleLogin());
        }

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v ->
                    Toast.makeText(LoginActivity.this, "Đăng nhập bằng Google...", Toast.LENGTH_SHORT).show()
            );
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v ->
                    Toast.makeText(LoginActivity.this, "Vui lòng liên hệ quản trị viên để khôi phục mật khẩu", Toast.LENGTH_SHORT).show()
            );
        }

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }

    private void handleLogin() {
        if (etPhone == null || etPassword == null) return;

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

        UserDao userDao = new UserDao(this);
        User user = userDao.getById("user_" + phone);

        if (user == null) {
            user = new User();
            user.setUserId("user_" + phone);
            user.setPhone(phone);
            user.setFamilyName("Hộ gia đình " + phone);
            user.setRole("PARENT");
            userDao.upsert(user);
        }

        SessionManager session = new SessionManager(this);
        session.createLoginSession(user);

        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
