package com.example.studenthousing.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.CampusRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.OtpHelper;
import com.example.studenthousing.ui.dialog.SearchListDialog;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputLayout tilParentName, tilPhone, tilUniversity;
    private TextInputEditText etParentName, etPhone, tvUniversity;
    private TextView tvHeaderTitle;
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
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupMode() {
        if (isEditMode) {
            tvHeaderTitle.setText("Chỉnh sửa thông tin");
            btnContinue.setText("Lưu thay đổi");

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
        if (btnContinue != null) btnContinue.setOnClickListener(v -> handleContinueOrSave());

        View.OnClickListener selectUniListener = v -> {
            List<Campus> campusList = CampusRepository.getInstance(this).getAllCampuses();
            String[] items = new String[campusList.size()];
            for (int i = 0; i < campusList.size(); i++) {
                items[i] = campusList.get(i).getDisplayName();
            }

            SearchListDialog.show(this, "Chọn trường đại học / cao đẳng (TP.HCM)", items, value -> {
                if (tvUniversity != null) {
                    tvUniversity.setText(value);
                    tvUniversity.setTextColor(ContextCompat.getColor(this, R.color.text_main));
                }
                if (tilUniversity != null) tilUniversity.setError(null);
            });
        };

        if (tilUniversity != null) tilUniversity.setOnClickListener(selectUniListener);
        if (tvUniversity != null) tvUniversity.setOnClickListener(selectUniListener);
    }

    private void clearErrors() {
        if (tilParentName != null) tilParentName.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
        if (tilUniversity != null) tilUniversity.setError(null);
    }

    private String cleanSelectedText(String selected) {
        if (selected == null) return "";
        String s = selected.trim();
        return s.startsWith("Chọn") ? "" : s;
    }

    // RegisterActivity.java

    private void handleContinueOrSave() {
        String parentName = etParentName != null && etParentName.getText() != null
                ? etParentName.getText().toString().trim() : "";
        String rawPhone = etPhone != null && etPhone.getText() != null
                ? etPhone.getText().toString().trim() : "";

        clearErrors();

        // Validate tên
        if (TextUtils.isEmpty(parentName)) {
            if (tilParentName != null) tilParentName.setError("Vui lòng nhập họ và tên");
            if (etParentName != null) etParentName.requestFocus();
            return;
        }

        // ===== BẮT BUỘC: phải có Firebase Auth trước khi tạo profile =====
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            // Chưa xác thực → bắt buộc quay về Login
            Toast.makeText(this,
                    "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.",
                    Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        // ===== UID LUÔN lấy từ Firebase, KHÔNG dùng customUid/user_ + timestamp =====
        String uid = fbUser.getUid();
        String phone = OtpHelper.toLocal(rawPhone);
        if (phone.isEmpty() && fbUser.getPhoneNumber() != null) {
            phone = OtpHelper.toLocal(fbUser.getPhoneNumber());
        }

        // Validate phone (chỉ khi đăng ký mới, không phải edit)
        if (!isEditMode) {
            if (TextUtils.isEmpty(phone)) {
                if (tilPhone != null) tilPhone.setError("Vui lòng nhập số điện thoại");
                return;
            }
            if (!OtpHelper.isValidVnPhone(phone)) {
                if (tilPhone != null) tilPhone.setError("SĐT không hợp lệ (10 số, bắt đầu bằng 0)");
                return;
            }
            // Kiểm tra trùng SĐT với user khác (không phải uid hiện tại)
            User existingByPhone = new UserDao(this).getByPhone(phone);
            if (existingByPhone != null
                    && !uid.equals(existingByPhone.getUserId())
                    && !phone.equals(verifiedPhone)) {
                if (tilPhone != null) tilPhone.setError("SĐT này đã được đăng ký");
                return;
            }
        }

        String university = tvUniversity != null && tvUniversity.getText() != null
                ? cleanSelectedText(tvUniversity.getText().toString()) : "";

        registerOrUpdateUser(uid, parentName, phone, university);
    }

    // RegisterActivity.java

    private void registerOrUpdateUser(String uid, String parentName, String phone, String university) {
        // Defensive: uid phải trùng Firebase UID hiện tại
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || !fbUser.getUid().equals(uid)) {
            Toast.makeText(this, "Phiên đăng nhập đã thay đổi. Vui lòng thử lại.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long now = System.currentTimeMillis();
        UserDao userDao = new UserDao(this);

        // Load user cũ nếu có (giữ các field đã set trước đó như avatar, identityType...)
        User user = userDao.getById(uid);
        if (user == null) {
            user = new User();
            user.setUserId(uid);
            user.setCreatedAt(now);
        }

        user.setFamilyName(parentName);
        if (phone != null && !phone.isEmpty()) user.setPhone(phone);
        if (university != null && !university.isEmpty()) user.setUniversityName(university);
        user.setUpdatedAt(now);

        // 1. Lưu local + session TRƯỚC (đảm bảo offline-first)
        userDao.upsert(user);
        session.createLoginSession(user);

        // 2. Đồng bộ Firestore (không cần anonymous auth nữa)
        new UserRepository(this).saveUserProfile(user, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d("RegisterActivity", "Profile saved to Firestore: " + uid);
                navigateHome();
            }

            @Override
            public void onError(Exception e) {
                // Lỗi mạng / rules → vẫn cho vào Home, SyncManager sẽ retry
                Log.w("RegisterActivity", "Firestore save failed, will retry later: "
                        + e.getMessage());
                navigateHome();
            }
        });
    }

    private void navigateHome() {
        runOnUiThread(() -> {
            String msg = isEditMode ? "Cập nhật thành công!" : "Đăng ký tài khoản thành công!";
            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
