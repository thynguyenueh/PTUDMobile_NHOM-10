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
import com.example.studenthousing.ui.viewmodel.KycViewModel;
import com.example.studenthousing.util.UiState;
import com.google.android.material.button.MaterialButton;

public class KycActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "kyc_prefs";
    public static final String KEY_STATUS = "kyc_status_";
    public static final String KEY_SUBMITTED_AT = "kyc_submitted_at_";

    private Uri parentFrontUri;
    private Uri parentBackUri;
    private Uri studentIdUri;

    private TextView tvParentIdStatus;
    private TextView tvStudentIdStatus;

    private View layoutParentPreviews;
    private View layoutParentFrontPreview;
    private View layoutParentBackPreview;
    private ImageView ivParentFrontPreview;
    private ImageView ivParentBackPreview;

    private View layoutStudentIdPreviews;
    private View layoutStudentIdPreview;
    private ImageView ivStudentIdPreview;

    private KycViewModel viewModel;
    private ActivityResultLauncher<String> parentFrontPicker;
    private ActivityResultLauncher<String> parentBackPicker;
    private ActivityResultLauncher<String> studentIdPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc);

        parentFrontPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                parentFrontUri = uri;
                updateDocumentStatus();
            }
        });
        parentBackPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                parentBackUri = uri;
                updateDocumentStatus();
            }
        });
        studentIdPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                studentIdUri = uri;
                updateDocumentStatus();
            }
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
        layoutStudentIdPreview = findViewById(R.id.layoutStudentIdPreview);
        ivStudentIdPreview = findViewById(R.id.ivStudentIdPreview);

        View btnRemoveParentFront = findViewById(R.id.btnRemoveParentFront);
        View btnRemoveParentBack = findViewById(R.id.btnRemoveParentBack);
        View btnRemoveStudentId = findViewById(R.id.btnRemoveStudentId);

        if (btnRemoveParentFront != null) {
            btnRemoveParentFront.setOnClickListener(v -> {
                parentFrontUri = null;
                updateDocumentStatus();
            });
        }
        if (btnRemoveParentBack != null) {
            btnRemoveParentBack.setOnClickListener(v -> {
                parentBackUri = null;
                updateDocumentStatus();
            });
        }
        if (btnRemoveStudentId != null) {
            btnRemoveStudentId.setOnClickListener(v -> {
                studentIdUri = null;
                updateDocumentStatus();
            });
        }

        com.google.android.material.checkbox.MaterialCheckBox cbConsent = findViewById(R.id.cbConsent);
        MaterialButton btnSubmitKyc = findViewById(R.id.btnSubmitKyc);

        View parentButton = findViewById(R.id.btnAddParentId);
        if (parentButton != null) {
            parentButton.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn ảnh CCCD phụ huynh")
                    .setItems(new String[]{"Mặt trước", "Mặt sau"}, (dialog, which) -> {
                        if (which == 0) parentFrontPicker.launch("image/*");
                        else parentBackPicker.launch("image/*");
                    }).show());
        }

        View studentButton = findViewById(R.id.btnAddStudentId);
        if (studentButton != null) studentButton.setOnClickListener(v -> studentIdPicker.launch("image/*"));

        if (btnSubmitKyc != null) {
            btnSubmitKyc.setOnClickListener(v -> {
                boolean consent = cbConsent != null && cbConsent.isChecked();
                viewModel.submitKyc(parentFrontUri, parentBackUri, studentIdUri, consent);
            });
        }

        updateDocumentStatus();
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
            tvStudentIdStatus.setText(studentIdUri != null ? "Đã chọn ảnh thẻ sinh viên" : getString(R.string.kyc_status_not_uploaded));
            tvStudentIdStatus.setTextColor(getResources().getColor(studentIdUri != null ? R.color.primary : R.color.text_sub, getTheme()));
        }

        // Cập nhật ảnh xem trước CCCD Phụ huynh
        boolean hasParentFront = parentFrontUri != null;
        boolean hasParentBack = parentBackUri != null;
        if (layoutParentPreviews != null) {
            layoutParentPreviews.setVisibility((hasParentFront || hasParentBack) ? View.VISIBLE : View.GONE);
        }
        if (layoutParentFrontPreview != null && ivParentFrontPreview != null) {
            layoutParentFrontPreview.setVisibility(hasParentFront ? View.VISIBLE : View.GONE);
            if (hasParentFront) {
                Glide.with(this).load(parentFrontUri).centerCrop().into(ivParentFrontPreview);
            }
        }
        if (layoutParentBackPreview != null && ivParentBackPreview != null) {
            layoutParentBackPreview.setVisibility(hasParentBack ? View.VISIBLE : View.GONE);
            if (hasParentBack) {
                Glide.with(this).load(parentBackUri).centerCrop().into(ivParentBackPreview);
            }
        }

        // Cập nhật ảnh xem trước Thẻ Sinh viên
        boolean hasStudentId = studentIdUri != null;
        if (layoutStudentIdPreviews != null && layoutStudentIdPreview != null && ivStudentIdPreview != null) {
            layoutStudentIdPreviews.setVisibility(hasStudentId ? View.VISIBLE : View.GONE);
            layoutStudentIdPreview.setVisibility(hasStudentId ? View.VISIBLE : View.GONE);
            if (hasStudentId) {
                Glide.with(this).load(studentIdUri).centerCrop().into(ivStudentIdPreview);
            }
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
