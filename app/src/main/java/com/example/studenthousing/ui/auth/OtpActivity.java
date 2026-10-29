package com.example.studenthousing.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OtpActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFICATION_ID = "verificationId";
    public static final String EXTRA_PHONE = "phoneNumber";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_PARENT_NAME = "PARENT_NAME";
    public static final String EXTRA_UNIVERSITY = "UNIVERSITY";
    public static final String EXTRA_GOOGLE_UID = "GOOGLE_UID";

    public static final String MODE_REGISTER = "REGISTER";
    public static final String MODE_LOGIN = "LOGIN";

    private String verificationId;
    private String phoneNumber;
    private String mode;
    private FirebaseAuth mAuth;
    private SessionManager session;

    private TextView tvOtpError;
    private EditText[] otpDigits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        verificationId = getIntent().getStringExtra(EXTRA_VERIFICATION_ID);
        phoneNumber = getIntent().getStringExtra(EXTRA_PHONE);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_REGISTER;

        if (verificationId == null || verificationId.isEmpty()) {
            Toast.makeText(this, "Phiên xác thực không hợp lệ, vui lòng thử lại", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvPhoneHint = findViewById(R.id.tvPhoneHint);
        if (tvPhoneHint != null) {
            tvPhoneHint.setText("Mã xác thực đã được gửi tới SĐT " + phoneNumber);
        }

        tvOtpError = null;
        otpDigits = new EditText[]{
                findViewById(R.id.otpDigit1), findViewById(R.id.otpDigit2), findViewById(R.id.otpDigit3),
                findViewById(R.id.otpDigit4), findViewById(R.id.otpDigit5), findViewById(R.id.otpDigit6)
        };
        for (int i = 0; i < otpDigits.length; i++) {
            final int index = i;
            EditText digit = otpDigits[i];
            digit.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                        && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && digit.length() == 0 && index > 0) {
                    otpDigits[index - 1].requestFocus();
                    otpDigits[index - 1].setSelection(otpDigits[index - 1].length());
                    return true;
                }
                return false;
            });
            digit.addTextChangedListener(new TextWatcher() {
                private boolean distributing;
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (distributing) return;
                    if (tvOtpError != null && tvOtpError.getVisibility() == View.VISIBLE) tvOtpError.setVisibility(View.GONE);
                    if (s.length() > 1) {
                        distributing = true;
                        String pasted = s.toString().replaceAll("\\D", "");
                        for (int j = 0; j < otpDigits.length; j++) {
                            otpDigits[j].setText(j < pasted.length() ? pasted.substring(j, j + 1) : "");
                        }
                        distributing = false;
                        otpDigits[Math.min(pasted.length(), otpDigits.length - 1)].requestFocus();
                    } else if (count > 0 && index < otpDigits.length - 1) {
                        otpDigits[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        MaterialButton btnVerify = findViewById(R.id.btnVerifyOtp);
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                StringBuilder enteredCode = new StringBuilder(6);
                for (EditText digit : otpDigits) enteredCode.append(digit.getText());
                String code = enteredCode.toString().trim();
                if (code.length() < 6) {
                    if (tvOtpError != null) {
                        tvOtpError.setText("Vui lòng nhập đủ 6 số OTP");
                        tvOtpError.setVisibility(View.VISIBLE);
                    }
                    for (EditText digit : otpDigits) {
                        if (digit.length() == 0) { digit.requestFocus(); break; }
                    }
                    return;
                }
                btnVerify.setEnabled(false);
                verifyCode(code, btnVerify);
            });
        }
    }

    private void verifyCode(String code, MaterialButton btnVerify) {
        PhoneAuthCredential phoneCredential = PhoneAuthProvider.getCredential(verificationId, code);

        mAuth.signInWithCredential(phoneCredential).addOnCompleteListener(this, task -> {
            btnVerify.setEnabled(true);
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Xác thực OTP thất bại: " +
                        (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser fbUser = mAuth.getCurrentUser();
            if (fbUser == null) {
                Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (MODE_LOGIN.equals(mode)) {
                checkExistingAndLogin(fbUser.getUid());
            } else {
                continueRegister(fbUser.getUid());
            }
        });
    }

    private void checkExistingAndLogin(String uid) {
        UserRepository repo = new UserRepository(this);
        repo.getUserProfile(uid, new AppCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    if (user.getPhone() == null || user.getPhone().isEmpty()) {
                        user.setPhone(phoneNumber);
                    }
                    repo.saveUserProfile(user, new AppCallback<Void>() {
                        @Override public void onSuccess(Void d) {
                            session.createLoginSession(user);
                            runOnUiThread(() -> {
                                Toast.makeText(OtpActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                goToHome();
                            });
                        }
                        @Override public void onError(Exception e) {
                            session.createLoginSession(user);
                            runOnUiThread(() -> {
                                Toast.makeText(OtpActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                goToHome();
                            });
                        }
                    });
                } else {
                    // Chưa có profile -> chuyển sang RegisterActivity để điền thông tin (Họ tên, Trường)
                    Intent i = new Intent(OtpActivity.this, RegisterActivity.class);
                    i.putExtra("VERIFIED_PHONE", phoneNumber);
                    i.putExtra("GOOGLE_UID", uid);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                // Tạo profile mới nếu chưa có
                continueRegister(uid);
            }
        });
    }

    private void continueRegister(String uid) {
        Intent in = getIntent();
        String parentName = in.getStringExtra(EXTRA_PARENT_NAME);
        String university = in.getStringExtra(EXTRA_UNIVERSITY);

        long now = System.currentTimeMillis();
        User user = new User();
        user.setUserId(uid);
        user.setPhone(phoneNumber);
        user.setFamilyName(parentName != null && !parentName.isEmpty() ? parentName : "Người dùng");
        if (university != null && !university.isEmpty()) {
            user.setUniversityName(university);
        }
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserRepository repo = new UserRepository(this);
        repo.saveUserProfile(user, new AppCallback<Void>() {
            @Override public void onSuccess(Void d) {
                session.createLoginSession(user);
                runOnUiThread(() -> {
                    Toast.makeText(OtpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    goToHome();
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(OtpActivity.this, "Đăng ký thất bại: Không thể kết nối máy chủ", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
