package com.example.studenthousing.ui.verify;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.databinding.ActivityResidenceVerificationBinding;
import com.example.studenthousing.ui.viewmodel.ResidenceVerificationViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResidenceVerificationActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "residence_prefs";
    public static final String KEY_STATUS = "status_";
    public static final String KEY_METHOD = "method_";
    public static final String KEY_FILE_PATH = "file_";
    public static final String KEY_FILE_NAME = "filename_";
    public static final String KEY_FILE_URL = "fileurl_";
    public static final String KEY_REG_NUMBER = "reg_";
    public static final String KEY_WARD = "ward_";
    public static final String KEY_SUBMITTED_AT = "at_";
    public static final String KEY_SYNCED = "synced_";

    private static final String[] WARDS = {
            "Phường Bến Nghé", "Phường Bến Thành", "Phường Đa Kao",
            "Phường 1", "Phường 2", "Phường 3", "Phường 25", "Phường 26"
    };

    private ResidenceVerificationViewModel viewModel;
    private ActivityResidenceVerificationBinding binding;
    private UserDao userDao;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private String userId;
    private String selectedFilePath = null;
    private String selectedFileName = null;
    private String selectedWard = null;
    private boolean alreadyVerified = false;

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
        userDao = new UserDao(this);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        viewModel.getStatusState().observe(this, this::renderStatusState);
        viewModel.getSubmitState().observe(this, this::renderSubmitState);

        setupListeners();
        selectOption(true);

        // Khôi phục lựa chọn đã lưu trước khi nạp trạng thái để tránh ghi đè UI
        loadSavedInputs();
        restoreFromState(savedInstanceState);
        loadStatus();
        if (selectedFilePath != null) {
            applyStatus(false, false);
        }
    }

    /**
     * Khôi phục các trường người dùng đã chọn sau khi bị hệ thống huỷ Activity
     * (xoay màn hình, đổi theme, bị thu hồi bộ nhớ...).
     */
    private void restoreFromState(Bundle state) {
        if (state == null) return;
        String path = state.getString("selectedFilePath", null);
        String name = state.getString("selectedFileName", null);
        String ward = state.getString("selectedWard", null);

        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                selectedFilePath = path;
                selectedFileName = name != null ? name : f.getName();
            }
        }
        if (ward != null && !ward.isEmpty()) {
            selectedWard = ward;
            binding.tvSelectedWard.setText(ward);
            binding.tvSelectedWard.setTextColor(getResources().getColor(R.color.text_main, getTheme()));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedFilePath", selectedFilePath);
        outState.putString("selectedFileName", selectedFileName);
        outState.putString("selectedWard", selectedWard);
    }

    private void loadSavedInputs() {
        String savedReg = prefs.getString(KEY_REG_NUMBER + userId, null);
        String savedWard = prefs.getString(KEY_WARD + userId, null);
        if (savedReg != null && !savedReg.isEmpty()) {
            binding.etRegistrationNumber.setText(savedReg);
        }
        if (savedWard != null && !savedWard.isEmpty()) {
            selectedWard = savedWard;
            binding.tvSelectedWard.setText(savedWard);
            binding.tvSelectedWard.setTextColor(getResources().getColor(R.color.text_main, getTheme()));
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnHelp.setOnClickListener(v -> showHelp());

        binding.rbOption1.setOnClickListener(v -> selectOption(true));
        binding.layoutOption1Header.setOnClickListener(v -> selectOption(true));
        binding.rbOption2.setOnClickListener(v -> selectOption(false));
        binding.layoutOption2Header.setOnClickListener(v -> selectOption(false));

        binding.rbOption1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectOption(true);
        });
        binding.rbOption2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectOption(false);
        });

        binding.layoutUploadBox.setOnClickListener(v -> {
            selectOption(true);
            pickFile.launch("*/*");
        });
        binding.layoutSelectWard.setOnClickListener(v -> {
            selectOption(false);
            showWardPicker();
        });
        binding.btnSubmitVerification.setOnClickListener(v -> {
            if (alreadyVerified) {
                Toast.makeText(this, "Bạn đã xác thực cư trú rồi", Toast.LENGTH_SHORT).show();
                return;
            }
            submit();
        });
    }

    private void selectOption(boolean option1) {
        binding.rbOption1.setChecked(option1);
        binding.rbOption2.setChecked(!option1);
        binding.layoutUploadBox.setAlpha(option1 ? 1f : 0.5f);
        binding.layoutOption2Inputs.setAlpha(option1 ? 0.5f : 1f);
        binding.etRegistrationNumber.setEnabled(!option1);
    }

    private void loadStatus() {
        if (viewModel != null && userId != null) {
            viewModel.loadStatus(userId);
        }
    }

    private void renderStatusState(UiState<ResidenceVerificationViewModel.ResidenceStatusData> state) {
        if (state == null || state.getContent() == null) return;
        ResidenceVerificationViewModel.ResidenceStatusData data = state.getContent();

        alreadyVerified = data.isVerified;
        String status = data.status;

        if (alreadyVerified || "APPROVED".equals(status)) {
            applyStatus(true, false);
        } else if ("PENDING".equals(status)) {
            applyStatus(false, true);
        } else {
            applyStatus(false, false);
        }
    }

    private void renderSubmitState(UiState<String> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.LOADING) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đang gửi...");
            return;
        }

        binding.btnSubmitVerification.setEnabled(true);
        binding.btnSubmitVerification.setText("Gửi hồ sơ đối soát");

        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi gửi hồ sơ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.getContent() != null) {
            Toast.makeText(this, state.getContent(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyStatus(boolean verified, boolean pending) {
        alreadyVerified = verified;
        String method = prefs.getString(KEY_METHOD + userId, "FILE");
        String fileUrl = prefs.getString(KEY_FILE_URL + userId, null);
        String regNum = prefs.getString(KEY_REG_NUMBER + userId, null);
        String wardName = prefs.getString(KEY_WARD + userId, null);

        if (verified) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đã xác thực cư trú");
        } else if (pending) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đang chờ Công an Phường đối soát (trong 24h)");
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
                runOnUiThread(() -> {
                    applyStatus(false, false);
                });
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

    private void showWardPicker() {
        com.example.studenthousing.util.ProvincePickerHelper.showPicker(this, (province, ward) -> {
            if (ward != null) {
                selectedWard = ward.getName();
            } else if (province != null) {
                selectedWard = province.getName();
            }
            if (selectedWard != null) {
                binding.tvSelectedWard.setText(selectedWard);
                binding.tvSelectedWard.setTextColor(
                        getResources().getColor(R.color.text_main, getTheme()));
            }
        });
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Xác thực cư trú")
                .setMessage("Tải lên Giấy xác nhận cư trú (Mẫu CT07) hoặc ảnh QR VNeID có mộc "
                        + "Công an Phường. Hoặc nhập số đăng ký cư trú và chọn Phường/Xã để đối soát. "
                        + "Sau khi được duyệt, bài đăng của bạn sẽ có tích xanh xác thực.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void submit() {
        boolean option1 = binding.rbOption1.isChecked();
        String reg = binding.etRegistrationNumber.getText().toString().trim();
        viewModel.submitVerification(userId, option1, selectedFilePath, selectedFileName, reg, selectedWard);
    }

    /**
     * Lưu lại lựa chọn của người dùng để lần mở sau khôi phục được ngay,
     * tránh mất tệp đã chọn khi tiến trình bị hệ thống thu hồi.
     */
    private void persistInputs() {
        if (prefs == null || userId == null) return;
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_METHOD + userId, binding.rbOption1.isChecked() ? "FILE" : "MANUAL");
        String reg = binding.etRegistrationNumber.getText().toString().trim();
        if (!reg.isEmpty()) editor.putString(KEY_REG_NUMBER + userId, reg);
        if (selectedWard != null) editor.putString(KEY_WARD + userId, selectedWard);
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
