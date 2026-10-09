package com.example.studenthousing;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivityResidenceVerificationBinding;

public class ResidenceVerificationActivity extends AppCompatActivity {

    private ActivityResidenceVerificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResidenceVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnHelp.setOnClickListener(v -> showHelpDialog());

        binding.rbOption1.setOnClickListener(v -> selectOption(1));
        binding.layoutOption1Header.setOnClickListener(v -> selectOption(1));

        binding.rbOption2.setOnClickListener(v -> selectOption(2));
        binding.layoutOption2Header.setOnClickListener(v -> selectOption(2));

        binding.layoutUploadBox.setOnClickListener(v -> {
            selectOption(1);
            Toast.makeText(this, "Mở máy ảnh / chọn tệp mẫu CT07", Toast.LENGTH_SHORT).show();
        });

        binding.layoutSelectWard.setOnClickListener(v -> showWardSelectionDialog());

        binding.btnSubmitVerification.setOnClickListener(v -> {
            Toast.makeText(this, "Đã gửi hồ sơ xác thực! Dữ liệu Phường sẽ đối soát trong 24h.", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void selectOption(int option) {
        if (option == 1) {
            binding.rbOption1.setChecked(true);
            binding.rbOption2.setChecked(false);
            binding.layoutUploadBox.setAlpha(1.0f);
            binding.layoutOption2Inputs.setAlpha(0.5f);
        } else {
            binding.rbOption1.setChecked(false);
            binding.rbOption2.setChecked(true);
            binding.layoutUploadBox.setAlpha(0.5f);
            binding.layoutOption2Inputs.setAlpha(1.0f);
        }
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác thực cư trú qua Công an Phường")
                .setMessage("Giấy xác nhận cư trú (Mẫu CT07) hoặc mã QR trên VNeID có mộc đỏ Công an Phường giúp tăng 90% độ tin cậy của bài đăng và giúp các gia đình yên tâm khi giao dịch.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showWardSelectionDialog() {
        String[] wards = {"Phường 25", "Phường 26", "Phường 27", "Phường 15", "Phường 17", "Phường Linh Trung", "Phường Thảo Điền"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Phường / Xã")
                .setItems(wards, (dialog, which) -> {
                    binding.tvSelectedWard.setText(wards[which]);
                    binding.tvSelectedWard.setTextColor(getResources().getColor(R.color.text_main, getTheme()));
                    selectOption(2);
                })
                .show();
    }
}
