package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivitySearchFilterBinding;
import com.google.android.material.slider.RangeSlider;

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

        resetFilters(); // Reset ban đầu, không chọn sẵn
        setupListeners();
        updateWardCount();
    }

    private void setupListeners() {
        binding.btnReset.setOnClickListener(v -> resetFilters());

        binding.layoutSelectProvince.setOnClickListener(v -> showProvinceDialog());
        binding.layoutSelectWard.setOnClickListener(v -> showWardDialog());

        binding.chipGroupWards.setOnCheckedStateChangeListener((group, checkedIds) -> updateWardCount());

        binding.sliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                int minPrice = Math.round(values.get(0));
                int maxPrice = Math.round(values.get(1));
                binding.tvMinPrice.setText(formatter.format(minPrice).replace(',', '.'));
                binding.tvMaxPrice.setText(formatter.format(maxPrice).replace(',', '.'));
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

    private void updateWardCount() {
        int count = binding.chipGroupWards.getCheckedChipIds().size();
        binding.tvWardCount.setText(getString(R.string.filter_ward_selected_count, count));
        updateResultButtonCount();
    }

    private void updateResultButtonCount() {
        int count = binding.chipGroupWards.getCheckedChipIds().size();
        int estimatedResults = Math.max(12, count * 62);
        binding.btnApplyFilter.setText(getString(R.string.filter_btn_apply, estimatedResults));
    }

    private void resetFilters() {
        binding.tvProvince.setText("Chọn Tỉnh / Thành phố");
        binding.tvWard.setText("Chọn Phường / Xã");

        binding.chipWard25.setChecked(false);
        binding.chipWard26.setChecked(false);
        binding.chipWard27.setChecked(false);
        binding.chipWard15.setChecked(false);
        binding.chipWard17.setChecked(false);

        binding.switchVerifiedOnly.setChecked(false);

        binding.sliderPrice.setValues(1000000f, 8000000f);
        binding.chipApartment.setChecked(false);

        updateWardCount();
        Toast.makeText(this, "Đã reset bộ lọc", Toast.LENGTH_SHORT).show();
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
