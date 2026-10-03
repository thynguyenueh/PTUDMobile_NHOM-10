package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class RegisterStep2Activity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvSkipHeader, tvSkipBottom;
    private EditText etHouseTitle, etDescription;
    private LinearLayout btnSelectDistrict, btnAddPhoto, layoutPhotoContainer;
    private ChipGroup chipGroupAmenities;
    private AppCompatButton btnFinishRegister;

    // Danh sách tiện ích mẫu
    private final String[] amenities = {
            "Wifi tốc độ cao", "Điều hòa", "Máy giặt",
            "Bếp nấu", "Chỗ để xe", "Ban công", "Tủ lạnh"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step2);

        initViews();
        setupAmenitiesChips();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSkipHeader = findViewById(R.id.tvSkipHeader);
        tvSkipBottom = findViewById(R.id.tvSkipBottom);
        etHouseTitle = findViewById(R.id.etHouseTitle);
        etDescription = findViewById(R.id.etDescription);
        btnSelectDistrict = findViewById(R.id.btnSelectDistrict);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        layoutPhotoContainer = findViewById(R.id.layoutPhotoContainer);
        chipGroupAmenities = findViewById(R.id.chipGroupAmenities);
        btnFinishRegister = findViewById(R.id.btnFinishRegister);
    }

    private void setupAmenitiesChips() {
        for (int i = 0; i < amenities.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(amenities[i]);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Mặc định chọn 3 item đầu tiên theo thiết kế Figma
            if (i < 3) {
                chip.setChecked(true);
            }

            chipGroupAmenities.addView(chip);
        }
    }

    private void setupEvents() {
        // Nút quay lại Step 1
        btnBack.setOnClickListener(v -> finish());

        // Nút Bỏ qua / Cập nhật sau
        tvSkipHeader.setOnClickListener(v -> finishRegistration());
        tvSkipBottom.setOnClickListener(v -> finishRegistration());

        // Chọn Quận / Huyện
        btnSelectDistrict.setOnClickListener(v ->
                Toast.makeText(this, "Chọn Quận/Huyện", Toast.LENGTH_SHORT).show()
        );

        // Thêm ảnh thực tế
        btnAddPhoto.setOnClickListener(v ->
                Toast.makeText(this, "Mở bộ sưu tập chọn ảnh", Toast.LENGTH_SHORT).show()
        );

        // Hoàn tất đăng ký
        btnFinishRegister.setOnClickListener(v -> finishRegistration());
    }

    private void finishRegistration() {
        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

        // Chuyển trực tiếp về màn hình chính MainActivity
        Intent intent = new Intent(RegisterStep2Activity.class.isInstance(this) ? this : RegisterStep2Activity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}