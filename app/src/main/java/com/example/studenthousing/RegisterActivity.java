package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class RegisterActivity extends AppCompatActivity {

    private ImageView btnBack, btnTogglePassword;
    private EditText etParentName, etPhone, etPassword, etConfirmPassword;
    private LinearLayout btnSelectUniversity;
    private TextView tvUniversity, tvLoginLink;
    private CheckBox cbTerms;
    private AppCompatButton btnContinue;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ Views
        initViews();

        // 2. Cài đặt các sự kiện Click & Xử lý Logic
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etParentName = findViewById(R.id.etParentName);
        etPhone = findViewById(R.id.etPhone);
        btnSelectUniversity = findViewById(R.id.btnSelectUniversity);
        tvUniversity = findViewById(R.id.tvUniversity);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private void setupEvents() {
        // Nút quay lại màn hình Đăng nhập
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bấm chọn Trường Đại học
        if (btnSelectUniversity != null) {
            btnSelectUniversity.setOnClickListener(v ->
                    Toast.makeText(RegisterActivity.this, "Chọn Trường Đại học", Toast.LENGTH_SHORT).show()
            );
        }

        // Ẩn / Hiện mật khẩu
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        // Bấm nút "Tiếp tục: Xác minh nhà ở" -> Chuyển sang RegisterStep2Activity
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> handleContinueToStep2());
        }

        // Bấm "Đã có tài khoản? Đăng nhập"
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> finish());
        }
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

    private void handleContinueToStep2() {
        String parentName = etParentName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(parentName)) {
            etParentName.setError("Vui lòng nhập họ và tên phụ huynh");
            etParentName.requestFocus();
            return;
        }

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
            etPassword.setError("Mật khẩu phải từ 8 ký tự trở lên");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không trùng khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với Điều khoản dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuyển sang Bước 2: Thông tin Chỗ ở
        Intent intent = new Intent(RegisterActivity.this, RegisterStep2Activity.class);
        startActivity(intent);
    }
}