package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivitySearchFilterBinding;

import java.text.DecimalFormat;
import java.util.List;

public class SearchFilterActivity extends AppCompatActivity {

    private ActivitySearchFilterBinding binding;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resetFilters();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReset.setOnClickListener(v -> resetFilters());

        binding.layoutSelectProvince.setOnClickListener(v -> showProvinceDialog());
        binding.layoutSelectWard.setOnClickListener(v -> showWardDialog());

        binding.sliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                int minPrice = Math.round(values.get(0));
                int maxPrice = Math.round(values.get(1));
                binding.etMinPrice.setText(String.valueOf(minPrice));
                binding.etMaxPrice.setText(String.valueOf(maxPrice));
            }
        });

        binding.btnApplyFilter.setOnClickListener(v -> {
            String province = binding.tvProvince.getText().toString();
            String ward = binding.tvWard.getText().toString();
            Intent intent = new Intent(SearchFilterActivity.this, SearchResultsActivity.class);
            intent.putExtra("PROVINCE", province);
            intent.putExtra("WARD", ward);
            startActivity(intent);
            finish();
        });
    }

    private void resetFilters() {
        binding.tvProvince.setText("Chọn Tỉnh / Thành phố");
        binding.tvWard.setText("Chọn Phường / Xã");

        binding.switchVerifiedOnly.setChecked(true);

        binding.sliderPrice.setValues(1000000f, 8000000f);
        binding.etMinPrice.setText("1000000");
        binding.etMaxPrice.setText("8000000");

        binding.chipApartment.setChecked(false);
        binding.chipWholeHouse.setChecked(false);
        binding.chipSharedRoom.setChecked(false);

        Toast.makeText(this, "Đã thiết lập lại bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void showProvinceDialog() {
        String[] cities = {"TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Bình Dương", "Cần Thơ"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Tỉnh / Thành phố")
                .setItems(cities, (dialog, which) -> binding.tvProvince.setText(cities[which]))
                .show();
    }

    private void showWardDialog() {
        String[] wards = {"Phường Thảo Điền", "Phường Bến Nghé", "Phường Linh Trung", "Phường An Phú", "Phường Tân Phong", "Phường Linh Xuân"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Phường / Xã")
                .setItems(wards, (dialog, which) -> binding.tvWard.setText(wards[which]))
                .show();
    }
}
