package com.example.studenthousing.ui.verify;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.ui.viewmodel.KycViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.android.material.button.MaterialButton;

public class KycActivity extends AppCompatActivity {

    private Uri parentFrontUri;
    private Uri parentBackUri;
    private Uri studentFrontUri;
    private Uri studentBackUri;

    private TextView tvParentIdStatus;
    private TextView tvStudentIdStatus;

    private View layoutParentPreviews;
    private View layoutParentFrontPreview;
    private View layoutParentBackPreview;
    private ImageView ivParentFrontPreview;
    private ImageView ivParentBackPreview;

    private View layoutStudentIdPreviews;
    private View layoutStudentFrontPreview;
    private View layoutStudentBackPreview;
    private ImageView ivStudentFrontPreview;
    private ImageView ivStudentBackPreview;

    private View addParentButton, addStudentButton;
    private MaterialButton btnSubmitKyc;
    private com.google.android.material.checkbox.MaterialCheckBox cbConsent;

    private KycViewModel viewModel;
    private ActivityResultLauncher<String> parentFrontPicker;
    private ActivityResultLauncher<String> parentBackPicker;
    private ActivityResultLauncher<String> studentFrontPicker;
    private ActivityResultLauncher<String> studentBackPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc);

        parentFrontPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { parentFrontUri = uri; updateDocumentStatus(); }
        });
        parentBackPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { parentBackUri = uri; updateDocumentStatus(); }
        });
        studentFrontPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { studentFrontUri = uri; updateDocumentStatus(); }
        });
        studentBackPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { studentBackUri = uri; updateDocumentStatus(); }
        });

        viewModel = new ViewModelProvider(this).get(KycViewModel.class);
        viewModel.getSubmitState().observe(this, this::renderSubmitState);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        tvParentIdStatus = findViewById(R.id.tvParentIdStatus);
        tvStudentIdStatus = findViewById(R.id.tvStudentIdStatus);

        layoutParentPreviews = findViewById(R.id.layoutParentPreviews);
        layoutParentFrontPreview = findViewById(R.id.layoutParentFrontPreview);
        layoutParentBackPreview = findViewById(R.id.layoutParentBackPreview);
        ivParentFrontPreview = findViewById(R.id.ivParentFrontPreview);
        ivParentBackPreview = findViewById(R.id.ivParentBackPreview);

        layoutStudentIdPreviews = findViewById(R.id.layoutStudentIdPreviews);
        layoutStudentFrontPreview = findViewById(R.id.layoutStudentFrontPreview);
        layoutStudentBackPreview = findViewById(R.id.layoutStudentBackPreview);
        ivStudentFrontPreview = findViewById(R.id.ivStudentFrontPreview);
        ivStudentBackPreview = findViewById(R.id.ivStudentBackPreview);

        View btnRemoveParentFront = findViewById(R.id.btnRemoveParentFront);
        View btnRemoveParentBack = findViewById(R.id.btnRemoveParentBack);
        View btnRemoveStudentFront = findViewById(R.id.btnRemoveStudentFront);
        View btnRemoveStudentBack = findViewById(R.id.btnRemoveStudentBack);

        if (btnRemoveParentFront != null) btnRemoveParentFront.setOnClickListener(v -> { parentFrontUri = null; updateDocumentStatus(); });
        if (btnRemoveParentBack != null) btnRemoveParentBack.setOnClickListener(v -> { parentBackUri = null; updateDocumentStatus(); });
        if (btnRemoveStudentFront != null) btnRemoveStudentFront.setOnClickListener(v -> { studentFrontUri = null; updateDocumentStatus(); });
        if (btnRemoveStudentBack != null) btnRemoveStudentBack.setOnClickListener(v -> { studentBackUri = null; updateDocumentStatus(); });

        btnSubmitKyc = findViewById(R.id.btnSubmitKyc);
        addParentButton = findViewById(R.id.btnAddParentId);
        addStudentButton = findViewById(R.id.btnAddStudentId);

        if (addParentButton != null) {
            addParentButton.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn ảnh CCCD phụ huynh")
                    .setItems(new String[]{"Mặt trước", "Mặt sau"}, (dialog, which) -> {
                        if (which == 0) parentFrontPicker.launch("image/*");
                        else parentBackPicker.launch("image/*");
                    }).show());
        }

        if (addStudentButton != null) {
            addStudentButton.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn ảnh Thẻ sinh viên")
                    .setItems(new String[]{"Mặt trước", "Mặt sau"}, (dialog, which) -> {
                        if (which == 0) studentFrontPicker.launch("image/*");
                        else studentBackPicker.launch("image/*");
                    }).show());
        }

        if (btnSubmitKyc != null) {
            btnSubmitKyc.setOnClickListener(v -> {
                boolean consent = cbConsent != null && cbConsent.isChecked();
                viewModel.submitKyc(parentFrontUri, parentBackUri, studentFrontUri, studentBackUri, consent);
            });
        }

        loadExistingUserData();
        updateDocumentStatus();
    }

    private void loadExistingUserData() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        UserDao userDao = new UserDao(this);
        User user = userDao.getById(userId);
        if (user != null) {
            if (user.getCitizenIdFrontUrl() != null) parentFrontUri = Uri.parse(user.getCitizenIdFrontUrl());
            if (user.getCitizenIdBackUrl() != null) parentBackUri = Uri.parse(user.getCitizenIdBackUrl());
            if (user.getStudentCardUrl() != null) {
                String card = user.getStudentCardUrl();
                if (card.contains("|")) {
                    String[] parts = card.split("\\|");
                    if (parts.length > 0 && !parts[0].isEmpty()) studentFrontUri = Uri.parse(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) studentBackUri = Uri.parse(parts[1]);
                } else {
                    studentFrontUri = Uri.parse(card);
                }
            }
            if (user.isIdentityVerified()) {
                if (btnSubmitKyc != null) {
                    btnSubmitKyc.setEnabled(false);
                    btnSubmitKyc.setText("Đã xác thực danh tính (KYC)");
                }
                if (addParentButton != null) addParentButton.setVisibility(View.GONE);
                if (addStudentButton != null) addStudentButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateDocumentStatus() {
        if (tvParentIdStatus != null) {
            boolean complete = parentFrontUri != null && parentBackUri != null;
            tvParentIdStatus.setText(complete ? "Đã chọn đủ 2 mặt CCCD" :
                    (parentFrontUri != null ? "Đã chọn mặt trước · Còn thiếu mặt sau" :
                            (parentBackUri != null ? "Đã chọn mặt sau · Còn thiếu mặt trước" : getString(R.string.kyc_status_not_uploaded))));
            tvParentIdStatus.setTextColor(getResources().getColor(complete ? R.color.primary : R.color.text_sub, getTheme()));
        }
        if (tvStudentIdStatus != null) {
            boolean complete = studentFrontUri != null && studentBackUri != null;
            tvStudentIdStatus.setText(complete ? "Đã chọn đủ 2 mặt Thẻ sinh viên" :
                    (studentFrontUri != null ? "Đã chọn mặt trước · Còn thiếu mặt sau" :
                            (studentBackUri != null ? "Đã chọn mặt sau · Còn thiếu mặt trước" : getString(R.string.kyc_status_not_uploaded))));
            tvStudentIdStatus.setTextColor(getResources().getColor(complete ? R.color.primary : R.color.text_sub, getTheme()));
        }

        // Cập nhật ảnh xem trước CCCD Phụ huynh
        boolean hasParentFront = parentFrontUri != null;
        boolean hasParentBack = parentBackUri != null;
        if (layoutParentPreviews != null) {
            layoutParentPreviews.setVisibility((hasParentFront || hasParentBack) ? View.VISIBLE : View.GONE);
        }
        if (layoutParentFrontPreview != null && ivParentFrontPreview != null) {
            layoutParentFrontPreview.setVisibility(hasParentFront ? View.VISIBLE : View.GONE);
            if (hasParentFront) Glide.with(this).load(parentFrontUri).centerCrop().into(ivParentFrontPreview);
        }
        if (layoutParentBackPreview != null && ivParentBackPreview != null) {
            layoutParentBackPreview.setVisibility(hasParentBack ? View.VISIBLE : View.GONE);
            if (hasParentBack) Glide.with(this).load(parentBackUri).centerCrop().into(ivParentBackPreview);
        }

        // Cập nhật ảnh xem trước Thẻ Sinh viên
        boolean hasStudentFront = studentFrontUri != null;
        boolean hasStudentBack = studentBackUri != null;
        if (layoutStudentIdPreviews != null) {
            layoutStudentIdPreviews.setVisibility((hasStudentFront || hasStudentBack) ? View.VISIBLE : View.GONE);
        }
        if (layoutStudentFrontPreview != null && ivStudentFrontPreview != null) {
            layoutStudentFrontPreview.setVisibility(hasStudentFront ? View.VISIBLE : View.GONE);
            if (hasStudentFront) Glide.with(this).load(studentFrontUri).centerCrop().into(ivStudentFrontPreview);
        }
        if (layoutStudentBackPreview != null && ivStudentBackPreview != null) {
            layoutStudentBackPreview.setVisibility(hasStudentBack ? View.VISIBLE : View.GONE);
            if (hasStudentBack) Glide.with(this).load(studentBackUri).centerCrop().into(ivStudentBackPreview);
        }
    }

    private void renderSubmitState(UiState<Boolean> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi xác minh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Boolean.TRUE.equals(state.getContent())) {
            Toast.makeText(this, "Hồ sơ KYC đã được gửi! Đang chờ đối soát trong 24h", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
