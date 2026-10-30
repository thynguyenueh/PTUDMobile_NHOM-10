package com.example.studenthousing.ui.verify;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.studenthousing.databinding.ActivityResidenceVerificationBinding;
import com.example.studenthousing.ui.viewmodel.ResidenceVerificationViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResidenceVerificationActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "residence_prefs";
    public static final String KEY_FILE_PATH = "file_";
    public static final String KEY_FILE_NAME = "filename_";
    public static final String KEY_STATUS = "status_";

    private ResidenceVerificationViewModel viewModel;
    private ActivityResidenceVerificationBinding binding;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private String userId;
    private String selectedFilePath = null;
    private String selectedFileName = null;

    private final ActivityResultLauncher<String> pickFile =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) copyPickedFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResidenceVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ResidenceVerificationViewModel.class);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = session.getUserId();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        viewModel.getStatusState().observe(this, this::renderStatusState);
        viewModel.getSubmitState().observe(this, this::renderSubmitState);

        setupListeners();

        loadSavedInputs();
        restoreFromState(savedInstanceState);
        updatePreview();
        loadStatus();
    }

    private void restoreFromState(Bundle state) {
        if (state == null) return;
        String path = state.getString("selectedFilePath", null);
        String name = state.getString("selectedFileName", null);

        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                selectedFilePath = path;
                selectedFileName = name != null ? name : f.getName();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedFilePath", selectedFilePath);
        outState.putString("selectedFileName", selectedFileName);
    }

    private void loadSavedInputs() {
        String savedPath = prefs.getString(KEY_FILE_PATH + userId, null);
        String savedName = prefs.getString(KEY_FILE_NAME + userId, null);

        if (savedPath != null) {
            File f = new File(savedPath);
            if (f.exists()) {
                selectedFilePath = savedPath;
                selectedFileName = savedName;
            }
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.layoutUploadBox.setOnClickListener(v -> pickFile.launch("*/*"));
        binding.btnSubmitVerification.setOnClickListener(v -> submit());
        binding.btnRemovePreview.setOnClickListener(v -> {
            selectedFilePath = null;
            selectedFileName = null;
            if (prefs != null && userId != null) {
                prefs.edit().remove(KEY_FILE_PATH + userId).remove(KEY_FILE_NAME + userId).apply();
            }
            updatePreview();
        });
    }

    private void updatePreview() {
        if (selectedFilePath != null) {
            File f = new File(selectedFilePath);
            if (f.exists()) {
                binding.cardPreview.setVisibility(View.VISIBLE);
                String name = selectedFileName != null ? selectedFileName : f.getName();
                binding.tvPreviewFileName.setText(name);
                binding.tvUploadText.setText("Đổi tệp khác (" + name + ")");

                Glide.with(this)
                        .load(f)
                        .centerCrop()
                        .into(binding.ivPreview);
                return;
            }
        }
        binding.cardPreview.setVisibility(View.GONE);
        binding.tvUploadText.setText("Bấm để chọn ảnh hoặc tệp");
    }

    private void loadStatus() {
        if (viewModel != null && userId != null) {
            viewModel.loadStatus(userId);
        }
    }

    private void renderStatusState(UiState<ResidenceVerificationViewModel.ResidenceStatusData> state) {
        if (state == null || state.getContent() == null) return;
        ResidenceVerificationViewModel.ResidenceStatusData data = state.getContent();

        if (data.isVerified) {
            binding.tvBannerText.setText("Tài khoản của bạn đã được xác thực cư trú");
            binding.btnSubmitVerification.setText("Đã xác thực cư trú");
            binding.btnSubmitVerification.setEnabled(true);
            if (prefs != null && userId != null) {
                prefs.edit().putString(KEY_STATUS + userId, "APPROVED").apply();
            }
        } else {
            binding.tvBannerText.setText("Tải lên giấy tờ cư trú để tự động duyệt xác thực");
            binding.btnSubmitVerification.setText("Xác thực cư trú ngay");
            binding.btnSubmitVerification.setEnabled(true);
        }
    }

    private void renderSubmitState(UiState<String> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.LOADING) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đang xử lý & duyệt tự động...");
            return;
        }

        binding.btnSubmitVerification.setEnabled(true);

        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            binding.btnSubmitVerification.setText("Xác thực cư trú ngay");
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi gửi hồ sơ", Toast.LENGTH_LONG).show();
            return;
        }

        if (state.getContent() != null) {
            Toast.makeText(this, state.getContent(), Toast.LENGTH_SHORT).show();
            if (prefs != null && userId != null) {
                prefs.edit().putString(KEY_STATUS + userId, "APPROVED").apply();
            }
            selectedFilePath = null;
            selectedFileName = null;
            updatePreview();
            loadStatus();
        }
    }

    private void copyPickedFile(Uri uri) {
        io.execute(() -> {
            try {
                String name = queryName(uri);
                File dir = new File(getFilesDir(), "residence");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new Exception("Không thể tạo thư mục lưu trữ: " + dir.getAbsolutePath());
                }
                File out = new File(dir, userId + "_" + System.currentTimeMillis() + "_" + name);
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(out)) {
                    if (in == null) throw new Exception("Không đọc được tệp");
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
                selectedFilePath = out.getAbsolutePath();
                selectedFileName = name;
                runOnUiThread(this::updatePreview);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Không thể chọn tệp: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String queryName(Uri uri) {
        String name = "ct07_" + System.currentTimeMillis();
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        }
        return name;
    }

    private void submit() {
        viewModel.submitVerification(userId, selectedFilePath);
    }

    private void persistInputs() {
        if (prefs == null || userId == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        if (selectedFilePath != null) editor.putString(KEY_FILE_PATH + userId, selectedFilePath);
        if (selectedFileName != null) editor.putString(KEY_FILE_NAME + userId, selectedFileName);
        editor.apply();
    }

    @Override
    protected void onPause() {
        persistInputs();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
