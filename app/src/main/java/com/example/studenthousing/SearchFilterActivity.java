package com.example.studenthousing;

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

        setupListeners();
        updateWardCount();
    }

    private void setupListeners() {
        binding.btnReset.setOnClickListener(v -> resetFilters());

        binding.layoutSelectProvince.setOnClickListener(v -> showProvinceDialog());
        binding.layoutSelectDistrict.setOnClickListener(v -> showDistrictDialog());

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
            int selectedWards = binding.chipGroupWards.getCheckedChipIds().size();
            boolean verifiedOnly = binding.switchVerifiedOnly.isChecked();
            String message = "Đã áp dụng bộ lọc (" + selectedWards + " phường, " +
                    (verifiedOnly ? "Đã xác thực" : "Tất cả") + ")";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        binding.tvProvince.setText(R.string.city_hcm);
        binding.tvDistrict.setText(R.string.district_binh_thanh);

        binding.chipWard25.setChecked(true);
        binding.chipWard26.setChecked(true);
        binding.chipWard27.setChecked(false);
        binding.chipWard15.setChecked(false);
        binding.chipWard17.setChecked(false);

        binding.switchVerifiedOnly.setChecked(true);

        binding.sliderPrice.setValues(2000000f, 8000000f);
        binding.chipApartment.setChecked(true);

        updateWardCount();
        Toast.makeText(this, "Đã thiết lập lại bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void showProvinceDialog() {
        String[] cities = {"TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Bình Dương", "Cần Thơ"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Tỉnh / Thành phố")
                .setItems(cities, (dialog, which) -> binding.tvProvince.setText(cities[which]))
                .show();
    }

    private void showDistrictDialog() {
        String[] districts = {"Quận Bình Thạnh", "TP. Thủ Đức", "Quận 1", "Quận 3", "Quận 7", "Quận Tân Bình"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Quận / Huyện")
                .setItems(districts, (dialog, which) -> binding.tvDistrict.setText(districts[which]))
                .show();
    }
}
