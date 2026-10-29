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
                    ? cleanSelectedText(tvUniversity.getText().toString()) : "";
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

        final String phone = OtpHelper.toLocal(rawPhone);
        UserDao userDao = new UserDao(this);
        if (!isEditMode && userDao.getByPhone(phone) != null && (verifiedPhone == null || !verifiedPhone.equals(phone))) {
            if (tilPhone != null) tilPhone.setError("Số điện thoại này đã được đăng ký");
            if (etPhone != null) etPhone.requestFocus();
            Toast.makeText(this, "Số điện thoại này đã được đăng ký!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String university = tvUniversity != null && tvUniversity.getText() != null ? cleanSelectedText(tvUniversity.getText().toString()) : "";
        String newUid = googleUid != null && !googleUid.isEmpty() ? googleUid : ("user_" + System.currentTimeMillis());
        registerOrUpdateUser(parentName, phone, university, newUid);
    }

    private void registerOrUpdateUser(String parentName, String phone, String university, String customUid) {
        long now = System.currentTimeMillis();
        UserDao userDao = new UserDao(this);
        final User user;
        String uid = (customUid != null && !customUid.isEmpty())
                ? customUid
                : session.getUserId();
        if (uid == null || uid.isEmpty()) {
            uid = "user_" + now;
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

        userDao.upsert(user);

        com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(authTask -> {
            com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                if (fbUser != null) {
                    user.setUserId(fbUser.getUid());
                } else {
                    user.setUserId("user_" + phone);
                }
            }
            userDao.upsert(user);
            session.createLoginSession(user);

            UserRepository userRepository = new UserRepository(this);
            userRepository.saveUserProfile(user, new AppCallback<Void>() {
                @Override public void onSuccess(Void d) {}
                @Override public void onError(Exception e) {}
            });

            runOnUiThread(() -> {
                String msg = isEditMode ? "Cập nhật thông tin thành công!" : "Đăng ký tài khoản thành công!";
                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
