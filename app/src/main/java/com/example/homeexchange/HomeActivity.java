package com.example.homeexchange;

import com.example.studenthousing.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivityHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Màn hình trang chủ (home). */
public class HomeActivity extends AppCompatActivity {

    private static final double NEAR_DISTANCE_KM = 2.0;

    private ActivityHomeBinding binding;
    private HouseAdapter houseAdapter;
    private List<House> allHouses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allHouses = createSampleHouses();

        setupHouseList();
        setupFilterChips();
        setupBottomNavigation();
        setupListeners();
        applyFilter(binding.chipGroupFilter.getCheckedChipId());
    }

    /** TODO: thay bằng dữ liệu thật (API/Database) khi merge. */
    private List<House> createSampleHouses() {
        return Arrays.asList(
                new House(getString(R.string.house_1_title), getString(R.string.house_1_address),
                        1.2, true, R.drawable.house_thao_dien),
                new House(getString(R.string.house_2_title), getString(R.string.house_2_address),
                        1.8, true, R.drawable.home_linh_trung),
                new House(getString(R.string.house_3_title), getString(R.string.house_3_address),
                        3.5, false, R.drawable.house_linh_xuan));
    }

    private void setupHouseList() {
        houseAdapter = new HouseAdapter(this::openHouseDetail);
        binding.rvHouses.setAdapter(houseAdapter);
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                applyFilter(checkedIds.get(0));
            }
        });
    }

    private void applyFilter(int checkedChipId) {
        List<House> filtered = new ArrayList<>();
        for (House house : allHouses) {
            if (matchesFilter(house, checkedChipId)) {
                filtered.add(house);
            }
        }
        houseAdapter.submitList(filtered);
    }

    private boolean matchesFilter(@NonNull House house, int checkedChipId) {
        if (checkedChipId == R.id.chipVerified) {
            return house.isVerified();
        }
        if (checkedChipId == R.id.chipNear) {
            return house.getDistanceKm() < NEAR_DISTANCE_KM;
        }
        return true;
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                return true;
            }
            // TODO: điều hướng sang các màn Đã lưu / Tin nhắn / Hồ sơ khi merge.
            showComingSoon();
            return false;
        });
    }

    private void setupListeners() {
        binding.btnNotifications.setOnClickListener(v -> showComingSoon());
        binding.btnFilter.setOnClickListener(v -> showComingSoon());
        binding.tvSeeAll.setOnClickListener(v -> showComingSoon());
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // TODO: gọi tìm kiếm theo trường ĐH khi merge.
                showComingSoon();
                return true;
            }
            return false;
        });
    }

    private void openHouseDetail(@NonNull House house) {
        // TODO: truyền id nhà qua Intent extra khi có dữ liệu thật.
        startActivity(new Intent(this, DetailActivity.class));
    }

    private void showComingSoon() {
        Toast.makeText(this, R.string.home_coming_soon, Toast.LENGTH_SHORT).show();
    }
}
