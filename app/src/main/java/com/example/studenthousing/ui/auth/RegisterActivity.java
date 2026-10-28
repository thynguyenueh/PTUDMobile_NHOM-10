package com.example.studenthousing.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.LocationData;
import com.example.studenthousing.util.OtpHelper;
import com.example.studenthousing.ui.dialog.SearchListDialog;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private static final String[] UNIVERSITIES = {
            "Đại học Bách Khoa Hà Nội (HUST)",
            "Đại học Quốc gia Hà Nội (VNU-HN)",
            "Đại học Kinh tế Quốc dân (NEU)",
            "Đại học Ngoại thương (FTU)",
            "Đại học Y Hà Nội (HMU)",
            "Học viện Công nghệ Bưu chính Viễn thông (PTIT)",
            "Đại học Quốc gia Thành phố Hồ Chí Minh (VNU-HCM)",
            "Trường Đại học Bách Khoa - ĐHQG TP.HCM (HCMUT)",
            "Trường Đại học Kinh tế TP. Hồ Chí Minh (UEH)",
            "Trường Đại học Khoa học Tự nhiên - ĐHQG TP.HCM (HCMUS)",
            "Trường Đại học Khoa học Xã hội và Nhân văn - ĐHQG TP.HCM (USSH)",
            "Trường Đại học Công nghệ Thông tin - ĐHQG TP.HCM (UIT)",
            "Trường Đại học Quốc tế - ĐHQG TP.HCM (IU)",
            "Trường Đại học Ngoại thương Cơ sở II (FTU CS2)",
            "Trường Đại học Sư phạm TP.HCM (HCMUE)",
            "Trường Đại học Y Dược TP.HCM (UMP)",
            "Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)",
            "Trường Đại học Tôn Đức Thắng (TDTU)",
            "Trường Đại học Công nghiệp TP.HCM (IUH)",
            "Trường Đại học Ngân hàng TP.HCM (HUB)",
            "Trường Đại học Luật TP.HCM (ULH)",
            "Trường Đại học Cần Thơ (CTU)",
            "Đại học Đà Nẵng (UD)",
            "Đại học Huế (HueU)",
            "Trường Đại học Nha Trang (NTU)",
            "Trường Đại học Mở TP.HCM (HCMOU)",
            "Trường Đại học Kinh tế - Luật ĐHQG TP.HCM (UEL)",
            "Trường Đại học FPT",
            "Trường Đại học RMIT Việt Nam",
            "Trường Đại học Văn Lang",
            "Trường Đại học Hoa Sen",
            "Trường Đại học Kiến trúc TP.HCM"
    };

    private ImageView btnBack;
    private TextInputLayout tilParentName, tilPhone, tilUniversity;
    private TextInputEditText etParentName, etPhone, tvUniversity;
    private TextView tvLoginLink, tvHeaderTitle;
    private MaterialCheckBox cbTerms;
    private MaterialButton btnContinue;

    private boolean isEditMode = false;
    private String verifiedPhone;
    private String googleUid;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        verifiedPhone = getIntent().getStringExtra("VERIFIED_PHONE");
        googleUid = getIntent().getStringExtra("GOOGLE_UID");

        initViews();
        setupMode();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tilParentName = findViewById(R.id.tilParentName);
        etParentName = findViewById(R.id.etParentName);
        tilPhone = findViewById(R.id.tilPhone);
        etPhone = findViewById(R.id.etPhone);
        tilUniversity = findViewById(R.id.tilUniversity);
        tvUniversity = findViewById(R.id.tvUniversity);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private void setupMode() {
        if (isEditMode) {
            tvHeaderTitle.setText("Chỉnh sửa thông tin");
            btnContinue.setText("Lưu thay đổi");
            tvLoginLink.setVisibility(View.GONE);
            cbTerms.setChecked(true);

            if (session.getUserPhone() != null && etPhone != null) etPhone.setText(session.getUserPhone());
            if (session.getUserFamilyName() != null && etParentName != null) etParentName.setText(session.getUserFamilyName());
            if (etPhone != null) {
                etPhone.setEnabled(false);
                if (tilPhone != null) tilPhone.setVisibility(View.GONE);
            }

            UserDao userDao = new UserDao(this);
            User u = userDao.getById(session.getUserId());
            if (u != null) {
                if (u.getUniversityName() != null && tvUniversity != null) {
                    tvUniversity.setText(u.getUniversityName());
                    tvUniversity.setTextColor(ContextCompat.getColor(this, R.color.text_main));
                }
            }
            return;
        }

        String googleName = getIntent().getStringExtra("GOOGLE_NAME");

        if (googleName != null && !googleName.trim().isEmpty() && etParentName != null && tilParentName != null) {
            etParentName.setText(googleName.trim());
            etParentName.setEnabled(false);
            tilParentName.setVisibility(View.GONE);
        }
        if (verifiedPhone != null && !verifiedPhone.isEmpty() && etPhone != null && tilPhone != null) {
            etPhone.setText(verifiedPhone);
            etPhone.setEnabled(false);
            tilPhone.setVisibility(View.GONE);
        }
    }

    private void setupEvents() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (tvLoginLink != null) tvLoginLink.setOnClickListener(v -> finish());
        if (btnContinue != null) btnContinue.setOnClickListener(v -> handleContinueOrSave());

        View.OnClickListener selectUniListener = v ->
                SearchListDialog.show(this, "Chọn trường đại học", UNIVERSITIES, value -> {
                    if (tvUniversity != null) {
                        tvUniversity.setText(value);
                        tvUniversity.setTextColor(ContextCompat.getColor(this, R.color.text_main));
                    }
                    if (tilUniversity != null) tilUniversity.setError(null);
                });

        if (tilUniversity != null) tilUniversity.setOnClickListener(selectUniListener);
        if (tvUniversity != null) tvUniversity.setOnClickListener(selectUniListener);
    }

    private void clearErrors() {
        if (tilParentName != null) tilParentName.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
        if (tilUniversity != null) tilUniversity.setError(null);
    }

    private void handleContinueOrSave() {
        String parentName = etParentName != null && etParentName.getText() != null ? etParentName.getText().toString().trim() : "";
        String rawPhone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        clearErrors();

        if (TextUtils.isEmpty(parentName)) {
            if (tilParentName != null) tilParentName.setError("Vui lòng nhập họ và tên");
            if (etParentName != null) etParentName.requestFocus();
            return;
        }

        if (isEditMode) {
            String phoneEdit = !TextUtils.isEmpty(rawPhone) ? OtpHelper.toLocal(rawPhone) : session.getUserPhone();
            String universityEdit = tvUniversity != null && tvUniversity.getText() != null
                    ? LocationData.clean(tvUniversity.getText().toString()) : "";
            registerOrUpdateUser(parentName, phoneEdit, universityEdit, session.getUserId());
            return;
        }

        if (TextUtils.isEmpty(rawPhone)) {
            if (tilPhone != null) tilPhone.setError("Vui lòng nhập số điện thoại");
            if (etPhone != null) etPhone.requestFocus();
            return;
        }
        if (!OtpHelper.isValidVnPhone(rawPhone)) {
            if (tilPhone != null) tilPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            if (etPhone != null) etPhone.requestFocus();
            return;
        }

        if (cbTerms != null && !cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với Điều khoản dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        final String phone = TextUtils.isEmpty(rawPhone) ? null : OtpHelper.toLocal(rawPhone);
        final String university = tvUniversity != null && tvUniversity.getText() != null ? LocationData.clean(tvUniversity.getText().toString()) : "";

        if (phone == null) {
            registerOrUpdateUser(parentName, null, university, googleUid);
            return;
        }

        sendRegistrationOtp(parentName, phone, university, googleUid);
    }

    private void sendRegistrationOtp(String parentName, String phone, String university, String googleUid) {
        OtpHelper.send(this, phone, new OtpHelper.SendCallback() {
            @Override public void onCodeSent(String id) {
                Toast.makeText(RegisterActivity.this, "Đã gửi mã OTP đến " + phone, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(RegisterActivity.this, OtpActivity.class);
                i.putExtra(OtpActivity.EXTRA_VERIFICATION_ID, id);
                i.putExtra(OtpActivity.EXTRA_PHONE, phone);
                i.putExtra(OtpActivity.EXTRA_MODE, OtpActivity.MODE_REGISTER);
                i.putExtra(OtpActivity.EXTRA_PARENT_NAME, parentName);
                i.putExtra(OtpActivity.EXTRA_UNIVERSITY, university);
                if (googleUid != null) i.putExtra(OtpActivity.EXTRA_GOOGLE_UID, googleUid);
                startActivity(i);
            }
            @Override public void onFailed(String msg) {
                Toast.makeText(RegisterActivity.this, "Lỗi gửi OTP: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerOrUpdateUser(String parentName, String phone, String university, String customUid) {
        long now = System.currentTimeMillis();
        UserDao userDao = new UserDao(this);
        final User user;
        String uid = (customUid != null && !customUid.isEmpty())
                ? customUid
                : session.getUserId();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Thiếu định danh người dùng Firebase", Toast.LENGTH_LONG).show();
            return;
        }

        User existing = userDao.getById(uid);
        if (existing == null) {
            user = new User();
            user.setUserId(uid);
            user.setCreatedAt(now);
        } else {
            user = existing;
        }
        if (phone != null) user.setPhone(phone);
        user.setFamilyName(parentName);
        if (university != null && !university.isEmpty()) {
            user.setUniversityName(university);
        }
        user.setUpdatedAt(now);

        UserRepository userRepository = new UserRepository(this);
        userRepository.saveUserProfile(user, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void d) {
                session.createLoginSession(user);
                runOnUiThread(() -> {
                    String msg = isEditMode ? "Cập nhật thông tin thành công!" : "Đăng ký tài khoản thành công!";
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    String err = isEditMode ? "Cập nhật thất bại: Không thể kết nối máy chủ" : "Đăng ký thất bại: Không thể kết nối máy chủ";
                    Toast.makeText(RegisterActivity.this, err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
