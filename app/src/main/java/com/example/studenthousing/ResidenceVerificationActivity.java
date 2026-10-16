package com.example.studenthousing;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.databinding.ActivityResidenceVerificationBinding;
import com.example.studenthousing.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResidenceVerificationActivity extends AppCompatActivity {

    // Lưu hồ sơ đang chờ duyệt (users_cache chỉ có 0/1 nên không phân biệt được "đang chờ")
    public static final String PREFS_NAME = "residence_prefs";
    public static final String KEY_STATUS = "status_";      // + userId -> "PENDING"
    public static final String KEY_METHOD = "method_";      // + userId -> "FILE" | "MANUAL"
    public static final String KEY_FILE_PATH = "file_";     // + userId -> đường dẫn file đã copy
    public static final String KEY_REG_NUMBER = "reg_";     // + userId
    public static final String KEY_WARD = "ward_";          // + userId
    public static final String KEY_SUBMITTED_AT = "at_";    // + userId

    private static final String[] WARDS = {
            "Phường Bến Nghé", "Phường Bến Thành", "Phường Đa Kao",
            "Phường 1", "Phường 2", "Phường 3", "Phường 25", "Phường 26"
    };

    private ActivityResidenceVerificationBinding binding;
    private SessionManager session;
    private UserDao userDao;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private String userId;
    private String selectedFilePath = null;   // file CT07 đã copy vào bộ nhớ app
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

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = session.getUserId();
        userDao = new UserDao(this);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupListeners();
        selectOption(true);
        loadStatus();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnHelp.setOnClickListener(v -> showHelp());

        binding.rbOption1.setOnClickListener(v -> selectOption(true));
        binding.layoutOption1Header.setOnClickListener(v -> selectOption(true));
        binding.rbOption2.setOnClickListener(v -> selectOption(false));
        binding.layoutOption2Header.setOnClickListener(v -> selectOption(false));

        binding.layoutUploadBox.setOnClickListener(v -> {
            selectOption(true);
            pickFile.launch("*/*");
        });
        binding.layoutSelectWard.setOnClickListener(v -> {
            selectOption(false);
            showWardPicker();
        });
        binding.btnSubmitVerification.setOnClickListener(v -> submit());
    }

    /** true = Option 1 (tải file CT07), false = Option 2 (nhập số đăng ký + chọn Phường). */
    private void selectOption(boolean option1) {
        binding.rbOption1.setChecked(option1);
        binding.rbOption2.setChecked(!option1);
        binding.layoutUploadBox.setAlpha(option1 ? 1f : 0.5f);
        binding.layoutOption2Inputs.setAlpha(option1 ? 0.5f : 1f);
        binding.etRegistrationNumber.setEnabled(!option1);
    }

    /** Kiểm tra trạng thái hiện tại: đã duyệt / đang chờ / chưa gửi. */
    private void loadStatus() {
        io.execute(() -> {
            User u = userDao.getById(userId);
            boolean verified = u != null && u.isResidenceVerified();
            boolean pending = "PENDING".equals(prefs.getString(KEY_STATUS + userId, null));
            runOnUiThread(() -> applyStatus(verified, pending));
        });
    }

    private void applyStatus(boolean verified, boolean pending) {
        alreadyVerified = verified;
        if (verified) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đã xác thực cư trú");
            binding.layoutDocCard.setVisibility(android.view.View.VISIBLE);
            binding.tvUploadedSectionTitle.setVisibility(android.view.View.VISIBLE);
            binding.imgDocCheck.setVisibility(android.view.View.VISIBLE);
            binding.tvDocStatus1.setText("Đã xác thực cư trú");
            binding.tvDocStatus2.setText("Thông tin cư trú đã được đối soát chính xác.");
            String savedPath = prefs.getString(KEY_FILE_PATH + userId, null);
            if (savedPath != null) {
                binding.tvFilename.setText(new File(savedPath).getName());
            }
        } else if (pending) {
            binding.btnSubmitVerification.setEnabled(false);
            binding.btnSubmitVerification.setText("Đang chờ Công an Phường đối soát (trong 24h)");
            binding.layoutDocCard.setVisibility(android.view.View.VISIBLE);
            binding.tvUploadedSectionTitle.setVisibility(android.view.View.VISIBLE);
            binding.imgDocCheck.setVisibility(android.view.View.GONE);
            binding.tvDocStatus1.setText("Đang chờ đối soát...");
            binding.tvDocStatus2.setText("Hồ sơ đã được gửi tới Công an Phường.");
            String savedPath = prefs.getString(KEY_FILE_PATH + userId, null);
            if (savedPath != null) {
                binding.tvFilename.setText(new File(savedPath).getName());
            }
        } else if (selectedFilePath != null) {
            binding.layoutDocCard.setVisibility(android.view.View.VISIBLE);
            binding.tvUploadedSectionTitle.setVisibility(android.view.View.VISIBLE);
            binding.imgDocCheck.setVisibility(android.view.View.GONE);
            binding.tvDocStatus1.setText("Tệp đã chuẩn bị gửi");
            binding.tvDocStatus2.setText("Bấm 'Gửi hồ sơ đối soát' bên dưới để hoàn tất.");
            if (selectedFileName != null) {
                binding.tvFilename.setText(selectedFileName);
            }
        } else {
            binding.layoutDocCard.setVisibility(android.view.View.GONE);
            binding.tvUploadedSectionTitle.setVisibility(android.view.View.GONE);
        }
    }

    private void copyPickedFile(Uri uri) {
        io.execute(() -> {
            try {
                String name = queryName(uri);
                File dir = new File(getFilesDir(), "residence");
                if (!dir.exists()) dir.mkdirs();
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
                    binding.tvFilename.setText(name);
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
        new AlertDialog.Builder(this)
                .setTitle("Chọn Phường/Xã")
                .setItems(WARDS, (d, which) -> {
                    selectedWard = WARDS[which];
                    binding.tvSelectedWard.setText(selectedWard);
                    binding.tvSelectedWard.setTextColor(
                            getResources().getColor(R.color.text_main, getTheme()));
                })
                .show();
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
        if (alreadyVerified) {
            Toast.makeText(this, "Tài khoản đã được xác thực cư trú", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean option1 = binding.rbOption1.isChecked();
        SharedPreferences.Editor ed = prefs.edit();

        if (option1) {
            if (selectedFilePath == null) {
                Toast.makeText(this, "Vui lòng tải lên Mẫu CT07 hoặc QR VNeID", Toast.LENGTH_SHORT).show();
                return;
            }
            ed.putString(KEY_METHOD + userId, "FILE")
                    .putString(KEY_FILE_PATH + userId, selectedFilePath);
        } else {
            String reg = binding.etRegistrationNumber.getText().toString().trim();
            if (reg.isEmpty()) {
                binding.etRegistrationNumber.setError("Nhập số đăng ký cư trú");
                return;
            }
            if (selectedWard == null) {
                Toast.makeText(this, "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
                return;
            }
            ed.putString(KEY_METHOD + userId, "MANUAL")
                    .putString(KEY_REG_NUMBER + userId, reg)
                    .putString(KEY_WARD + userId, selectedWard);
        }

        ed.putString(KEY_STATUS + userId, "PENDING")
                .putLong(KEY_SUBMITTED_AT + userId, System.currentTimeMillis())
                .apply();

        applyStatus(false, true);
        Toast.makeText(this,
                "Đã nhận hồ sơ. Đang chờ Công an Phường đối soát (trong 24h)",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
