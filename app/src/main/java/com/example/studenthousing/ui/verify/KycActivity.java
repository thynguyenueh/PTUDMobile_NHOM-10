package com.example.studenthousing.ui.verify;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.ui.adapter.SelectedPhotoAdapter;
import com.example.studenthousing.ui.viewmodel.KycViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KycActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "kyc_prefs";
    public static final String KEY_STATUS = "status_";

    private Uri parentFrontUri, parentBackUri, studentFrontUri, studentBackUri;
    private boolean isVerified = false; // Cờ đánh dấu đã xác thực

    private TextView tvParentIdStatus, tvStudentIdStatus;
    private RecyclerView rvParentPhotos, rvStudentPhotos;
    private SelectedPhotoAdapter parentAdapter, studentAdapter;
    private View addParentButton, addStudentButton;
    private MaterialButton btnSubmitKyc;
    private com.google.android.material.checkbox.MaterialCheckBox cbConsent;

    private KycViewModel viewModel;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String> parentFrontPicker, parentBackPicker, studentFrontPicker, studentBackPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc);

        initPickers();
        initViews();
        setupRecyclerViews();
        setupListeners();

        viewModel = new ViewModelProvider(this).get(KycViewModel.class);
        viewModel.getSubmitState().observe(this, this::renderSubmitState);

        loadExistingUserDataAsync();
    }

    private void initPickers() {
        parentFrontPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { parentFrontUri = uri; refreshParentRecycler(); }
        });
        parentBackPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { parentBackUri = uri; refreshParentRecycler(); }
        });
        studentFrontPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { studentFrontUri = uri; refreshStudentRecycler(); }
        });
        studentBackPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { studentBackUri = uri; refreshStudentRecycler(); }
        });
    }

    private void initViews() {
        if (findViewById(R.id.btnBack) != null) findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvParentIdStatus = findViewById(R.id.tvParentIdStatus);
        tvStudentIdStatus = findViewById(R.id.tvStudentIdStatus);
        rvParentPhotos = findViewById(R.id.rvParentPhotos);
        rvStudentPhotos = findViewById(R.id.rvStudentPhotos);
        btnSubmitKyc = findViewById(R.id.btnSubmitKyc);
        addParentButton = findViewById(R.id.btnAddParentId);
        addStudentButton = findViewById(R.id.btnAddStudentId);
        cbConsent = findViewById(R.id.cbConsent);
    }

    private void setupRecyclerViews() {
        parentAdapter = new SelectedPhotoAdapter(position -> {
            if (position == 0) parentFrontUri = null; else parentBackUri = null;
            refreshParentRecycler();
        });
        rvParentPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvParentPhotos.setAdapter(parentAdapter);

        studentAdapter = new SelectedPhotoAdapter(position -> {
            if (position == 0) studentFrontUri = null; else studentBackUri = null;
            refreshStudentRecycler();
        });
        rvStudentPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStudentPhotos.setAdapter(studentAdapter);
    }

    private void setupListeners() {
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
                btnSubmitKyc.setEnabled(false);
                btnSubmitKyc.setText("Đang xử lý...");
                boolean consent = cbConsent == null || cbConsent.isChecked();
                viewModel.submitKyc(parentFrontUri, parentBackUri, studentFrontUri, studentBackUri, consent);
            });
        }
    }

    private void refreshParentRecycler() {
        List<Uri> uris = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (parentFrontUri != null) { uris.add(parentFrontUri); labels.add("Mặt trước"); }
        if (parentBackUri != null) { uris.add(parentBackUri); labels.add("Mặt sau"); }
        parentAdapter.setData(uris, labels);
        updateStatusText(tvParentIdStatus, uris.size(), "CCCD");
    }

    private void refreshStudentRecycler() {
        List<Uri> uris = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (studentFrontUri != null) { uris.add(studentFrontUri); labels.add("Mặt trước"); }
        if (studentBackUri != null) { uris.add(studentBackUri); labels.add("Mặt sau"); }
        studentAdapter.setData(uris, labels);
        updateStatusText(tvStudentIdStatus, uris.size(), "Thẻ SV");
    }

    private void updateStatusText(TextView tv, int count, String type) {
        if (tv == null) return;
        if (isVerified) {
            tv.setText("Đã xác thực");
            tv.setTextColor(getResources().getColor(R.color.primary, getTheme()));
            return;
        }
        if (count == 0) {
            tv.setText(getString(R.string.kyc_status_not_uploaded));
            tv.setTextColor(getResources().getColor(R.color.text_sub, getTheme()));
        } else if (count == 2) {
            tv.setText("Đã chọn đủ 2 mặt " + type);
            tv.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        } else {
            tv.setText("Đã chọn 1 mặt · Còn thiếu 1 mặt");
            tv.setTextColor(getResources().getColor(R.color.text_sub, getTheme()));
        }
    }

    private void loadExistingUserDataAsync() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;

        ioExecutor.execute(() -> {
            UserDao userDao = new UserDao(this);
            User user = userDao.getById(userId);
            mainHandler.post(() -> {
                if (user != null) {
                    // Kiểm tra trạng thái xác thực
                    isVerified = user.isIdentityVerified();

                    // Load ảnh CCCD
                    if (user.getCitizenIdFrontUrl() != null) parentFrontUri = safeParseUri(user.getCitizenIdFrontUrl());
                    if (user.getCitizenIdBackUrl() != null) parentBackUri = safeParseUri(user.getCitizenIdBackUrl());

                    // Load ảnh Thẻ SV (có thể lưu dạng "front|back")
                    if (user.getStudentCardUrl() != null) {
                        String card = user.getStudentCardUrl();
                        if (card.contains("|")) {
                            String[] parts = card.split("\\|");
                            if (parts.length > 0) studentFrontUri = safeParseUri(parts[0]);
                            if (parts.length > 1) studentBackUri = safeParseUri(parts[1]);
                        } else {
                            studentFrontUri = safeParseUri(card);
                        }
                    }

                    // Cập nhật UI
                    refreshParentRecycler();
                    refreshStudentRecycler();

                    // Nếu đã xác thực, khóa các nút thêm/xóa
                    if (isVerified) {
                        if (btnSubmitKyc != null) {
                            btnSubmitKyc.setEnabled(false);
                            btnSubmitKyc.setText("Đã xác thực danh tính (KYC)");
                        }
                        if (addParentButton != null) addParentButton.setVisibility(View.GONE);
                        if (addStudentButton != null) addStudentButton.setVisibility(View.GONE);
                        if (cbConsent != null) cbConsent.setEnabled(false);

                        // Ẩn nút X trên từng item ảnh
                        parentAdapter.setRemoveEnabled(false);
                        studentAdapter.setRemoveEnabled(false);
                    }
                } else {
                    // Không có dữ liệu, hiển thị trống
                    refreshParentRecycler();
                    refreshStudentRecycler();
                }
            });
        });
    }

    private Uri safeParseUri(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            Uri uri = Uri.parse(url);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                java.io.File f = new java.io.File(uri.getPath());
                if (f.exists()) return uri;
                return null;
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    private void renderSubmitState(UiState<Boolean> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.LOADING) return;

        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            if (btnSubmitKyc != null) {
                btnSubmitKyc.setEnabled(true);
                btnSubmitKyc.setText("Gửi xác thực");
            }
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi xác minh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Boolean.TRUE.equals(state.getContent())) {
            isVerified = true;
            SessionManager session = new SessionManager(this);
            String userId = session.getUserId();
            if (userId != null) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putString(KEY_STATUS + userId, "APPROVED").apply();
            }
            Toast.makeText(this, "Xác thực danh tính thành công!", Toast.LENGTH_LONG).show();

            // Khóa các nút thêm/xóa sau khi xác thực
            if (addParentButton != null) addParentButton.setVisibility(View.GONE);
            if (addStudentButton != null) addStudentButton.setVisibility(View.GONE);
            if (cbConsent != null) cbConsent.setEnabled(false);
            if (btnSubmitKyc != null) {
                btnSubmitKyc.setEnabled(false);
                btnSubmitKyc.setText("Đã xác thực danh tính (KYC)");
            }
            parentAdapter.setRemoveEnabled(false);
            studentAdapter.setRemoveEnabled(false);
            updateStatusText(tvParentIdStatus, parentAdapter.getUris().size(), "CCCD");
            updateStatusText(tvStudentIdStatus, studentAdapter.getUris().size(), "Thẻ SV");

            // Không finish() nữa để người dùng thấy ảnh đã lưu
            // finish();
        } else {
            if (btnSubmitKyc != null && !btnSubmitKyc.getText().toString().contains("Đã xác thực")) {
                btnSubmitKyc.setEnabled(true);
                btnSubmitKyc.setText("Gửi xác thực");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}