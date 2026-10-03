package com.example.studenthousing;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etPhone = findViewById(R.id.etPhone);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegisterHousehold = findViewById(R.id.tvRegisterHousehold);

        btnLogin.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (phone.isEmpty()) {
                etPhone.setError("Vui lòng nhập số điện thoại");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }

            Toast.makeText(MainActivity.this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
        });

        tvRegisterHousehold.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở trang Đăng ký Hộ gia đình", Toast.LENGTH_SHORT).show()
        );
    }
}