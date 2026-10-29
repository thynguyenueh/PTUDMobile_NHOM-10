package com.example.studenthousing.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.repository.CampusRepository;
import com.example.studenthousing.databinding.ActivityFilterBinding;
import com.example.studenthousing.domain.FilterCriteria;
import com.example.studenthousing.ui.dialog.SearchListDialog;
import com.google.android.material.chip.Chip;

import java.util.List;

public class SearchFilterActivity extends AppCompatActivity {

    private ActivityFilterBinding binding;
    private String selectedCampusId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUtilitiesFromAsset();
        setupListeners();

        FilterCriteria criteria = IntentCompat.getSerializableExtra(
                getIntent(),
                FilterCriteria.EXTRA,
                FilterCriteria.class
        );

        if (criteria != null) {
            restoreFilterCriteria(criteria);
        } else {
            resetFiltersWithoutToast();
        }

        updateResultButtonCount();
    }

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        binding.btnReset.setOnClickListener(v -> resetFilters());
        binding.layoutSelectWard.setOnClickListener(v -> showWardDialog());
        binding.layoutSelectCampus.setOnClickListener(v -> showCampusDialog());

        binding.etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateResultButtonCount(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        for (int i = 0; i < binding.chipGroupDistance.getChildCount(); i++) {
            View child = binding.chipGroupDistance.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setOnCheckedChangeListener((buttonView, isChecked) -> updateResultButtonCount());
            }
        }

        for (int i = 0; i < binding.chipGroupFilterUtilities.getChildCount(); i++) {
            View child = binding.chipGroupFilterUtilities.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setOnCheckedChangeListener((buttonView, isChecked) -> updateResultButtonCount());
            }
        }

        binding.btnApplyFilter.setOnClickListener(v -> {
            FilterCriteria criteria = buildFilterCriteria();
            Intent intent = new Intent();
            intent.putExtra(FilterCriteria.EXTRA, criteria);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private FilterCriteria buildFilterCriteria() {
        FilterCriteria criteria = new FilterCriteria();

        criteria.searchQuery = binding.etSearchQuery.getText() != null ? binding.etSearchQuery.getText().toString().trim() : "";

        String wardStr = binding.tvWard.getText().toString();
        if (!wardStr.equals("Chọn Phường / Xã") && !wardStr.isEmpty()) {
            criteria.wards.add(wardStr);
        }

        criteria.campusId = selectedCampusId;

        int distCheckedId = binding.chipGroupDistance.getCheckedChipId();
        if (distCheckedId == R.id.chipDist1) criteria.maxDistanceKm = 1.0;
        else if (distCheckedId == R.id.chipDist2) criteria.maxDistanceKm = 2.0;
        else if (distCheckedId == R.id.chipDist3) criteria.maxDistanceKm = 3.0;
        else if (distCheckedId == R.id.chipDist5) criteria.maxDistanceKm = 5.0;
        else criteria.maxDistanceKm = 0.0;

        for (int i = 0; i < binding.chipGroupFilterUtilities.getChildCount(); i++) {
            View child = binding.chipGroupFilterUtilities.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    criteria.selectedUtilities.add(chip.getText().toString());
                }
            }
        }

        return criteria;
    }

    private void restoreFilterCriteria(FilterCriteria criteria) {
        if (criteria == null) {
            resetFiltersWithoutToast();
            return;
        }

        binding.etSearchQuery.setText(criteria.searchQuery != null ? criteria.searchQuery : "");

        if (criteria.wards != null && !criteria.wards.isEmpty()) {
            binding.tvWard.setText(criteria.wards.get(0));
            binding.tvWard.setTextColor(getColor(R.color.text_main));
        } else {
            binding.tvWard.setText("Chọn Phường / Xã");
            binding.tvWard.setTextColor(getColor(R.color.text_muted));
        }

        if (criteria.campusId != null && !criteria.campusId.isEmpty()) {
            Campus c = CampusRepository.getInstance(this).findByCode(criteria.campusId);
            if (c != null) {
                selectedCampusId = c.getCode();
                binding.tvCampus.setText(c.getDisplayName());
                binding.tvCampus.setTextColor(getColor(R.color.text_main));
            }
        } else {
            selectedCampusId = null;
            binding.tvCampus.setText("Chọn trường muốn ở gần");
            binding.tvCampus.setTextColor(getColor(R.color.text_muted));
        }

        if (criteria.maxDistanceKm == 1.0) binding.chipDist1.setChecked(true);
        else if (criteria.maxDistanceKm == 2.0) binding.chipDist2.setChecked(true);
        else if (criteria.maxDistanceKm == 3.0) binding.chipDist3.setChecked(true);
        else if (criteria.maxDistanceKm == 5.0) binding.chipDist5.setChecked(true);
        else binding.chipDistAny.setChecked(true);

        if (criteria.selectedUtilities != null) {
            for (int i = 0; i < binding.chipGroupFilterUtilities.getChildCount(); i++) {
                View child = binding.chipGroupFilterUtilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setChecked(criteria.selectedUtilities.contains(chip.getText().toString()));
                }
            }
        }
    }

    private void setupUtilitiesFromAsset() {
        if (binding.chipGroupFilterUtilities == null) return;
        List<com.example.studenthousing.data.model.Utility> utilities =
                com.example.studenthousing.data.loader.UtilitiesDataLoader.loadUtilities(this);
        for (com.example.studenthousing.data.model.Utility u : utilities) {
            boolean exists = false;
            for (int i = 0; i < binding.chipGroupFilterUtilities.getChildCount(); i++) {
                View child = binding.chipGroupFilterUtilities.getChildAt(i);
                if (child instanceof Chip && ((Chip) child).getText().toString().equalsIgnoreCase(u.getLabel())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Chip chip = new Chip(new android.view.ContextThemeWrapper(this, R.style.Widget_App_Chip), null, 0);
                chip.setText(u.getLabel());
                chip.setCheckable(true);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> updateResultButtonCount());
                binding.chipGroupFilterUtilities.addView(chip);
            }
        }
    }

    private void updateResultButtonCount() {
        FilterCriteria criteria = buildFilterCriteria();
        new Thread(() -> {
            PostDao postDao = new PostDao(this);
            List<AccommodationPost> all = postDao.getAll();

            int count = 0;
            for (AccommodationPost p : all) {
                if (criteria.matches(p)) count++;
            }
            int finalCount = count;
            runOnUiThread(() -> binding.btnApplyFilter.setText(getString(R.string.filter_btn_apply, finalCount)));
        }).start();
    }

    private void resetFiltersWithoutToast() {
        binding.etSearchQuery.setText("");
        binding.tvWard.setText("Chọn Phường / Xã");
        binding.tvWard.setTextColor(getColor(R.color.text_muted));
        selectedCampusId = null;
        binding.tvCampus.setText("Chọn trường muốn ở gần");
        binding.tvCampus.setTextColor(getColor(R.color.text_muted));
        binding.chipDistAny.setChecked(true);

        for (int i = 0; i < binding.chipGroupFilterUtilities.getChildCount(); i++) {
            View child = binding.chipGroupFilterUtilities.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setChecked(false);
            }
        }

        updateResultButtonCount();
    }

    private void resetFilters() {
        resetFiltersWithoutToast();
        Toast.makeText(this, "Đã reset bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void showWardDialog() {
        showProvincePicker();
    }

    private void showProvincePicker() {
        com.example.studenthousing.util.ProvincePickerHelper.showPicker(this, (cityName, ward) -> {
            if (ward != null) {
                binding.tvWard.setText(ward.getName());
                binding.tvWard.setTextColor(getColor(R.color.text_main));
            } else if (cityName != null) {
                binding.tvWard.setText(cityName);
                binding.tvWard.setTextColor(getColor(R.color.text_main));
            }
            updateResultButtonCount();
        });
    }

    private void showCampusDialog() {
        List<Campus> campuses = CampusRepository.getInstance(this).getAllCampuses();
        String[] items = new String[campuses.size()];
        for (int i = 0; i < campuses.size(); i++) {
            Campus c = campuses.get(i);
            items[i] = c.getDisplayName();
        }
        SearchListDialog.show(this, "Chọn trường đại học / cao đẳng", items, value -> {
            for (Campus c : campuses) {
                if (c.getDisplayName().equals(value)) {
                    selectedCampusId = c.getCode();
                    binding.tvCampus.setText(value);
                    binding.tvCampus.setTextColor(getColor(R.color.text_main));
                    break;
                }
            }
            updateResultButtonCount();
        });
    }
}
