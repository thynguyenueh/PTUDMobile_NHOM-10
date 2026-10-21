package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private HouseAdapter houseAdapter;
    private List<House> allHouses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupList();
        setupListeners();
        loadHouses();

        String ward = getIntent().getStringExtra("WARD");
        if (ward != null && !ward.equals("Chọn Phường / Xã")) {
            binding.etSearchQuery.setText(ward);
            filterHouses(ward);
        } else {
            filterHouses("");
        }
    }

    private void setupList() {
        houseAdapter = new HouseAdapter(house -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("house_id", house.getPostId());
            intent.putExtra("house_title", house.getTitle());
            intent.putExtra("house_address", house.getAddress());
            intent.putExtra("house_verified", house.isVerified());
            intent.putExtra("house_distance", house.getDistanceKm());
            intent.putExtra("house_price", house.getPrice());
            intent.putExtra("house_area", house.getArea());
            intent.putExtra("house_description", house.getDescription());
            intent.putExtra("house_host_name", house.getHostName());
            intent.putExtra("house_type", house.getHouseType());
            intent.putExtra("house_photo_res", house.getPhotoRes());
            startActivity(intent);
        });
        binding.rvResults.setAdapter(houseAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnFilter.setOnClickListener(v ->
                startActivity(new Intent(this, SearchFilterActivity.class)));

        binding.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearchQuery.getText().toString().trim();
                filterHouses(query);
                return true;
            }
            return false;
        });

        binding.chipVerified.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilter());
        binding.chipCheap.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilter());
        binding.chipApartment.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilter());
    }

    private void loadHouses() {
        allHouses = Arrays.asList(
                new House(
                        "house_1",
                        "Căn hộ cao cấp Thảo Điền",
                        "Phường Thảo Điền, Quận 2, TP. Thủ Đức",
                        1.2,
                        true,
                        R.drawable.house_thao_dien,
                        5.0,
                        65.0,
                        "Căn hộ cao cấp đầy đủ tiện nghi, view đẹp, bảo vệ 24/7.",
                        "Hộ gia đình Lê Văn Nam",
                        "Căn hộ"
                ),
                new House(
                        "house_2",
                        "Nhà trọ sinh viên Linh Trung",
                        "Phường Linh Trung, TP. Thủ Đức",
                        1.8,
                        true,
                        R.drawable.home_linh_trung,
                        3.2,
                        40.0,
                        "Phòng trọ rộng rãi có gác lửng, toilet riêng, giờ giấc tự do.",
                        "Hộ gia đình Trần Thị Mai",
                        "Phòng ở ghép"
                ),
                new House(
                        "house_3",
                        "Nhà nguyên căn Linh Xuân",
                        "Phường Linh Xuân, TP. Thủ Đức",
                        3.5,
                        false,
                        R.drawable.house_linh_xuan,
                        4.5,
                        80.0,
                        "Nhà nguyên căn 1 trệt 1 lầu, 2 phòng ngủ, bếp rộng.",
                        "Hộ gia đình Phạm Hoàng",
                        "Nhà nguyên căn"
                )
        );
    }

    private void filterHouses(String query) {
        binding.layoutIdle.setVisibility(View.GONE);
        applyFilter();
    }

    private void applyFilter() {
        String query = binding.etSearchQuery.getText() != null ?
                binding.etSearchQuery.getText().toString().trim().toLowerCase() : "";

        boolean verifiedOnly = binding.chipVerified.isChecked();
        boolean cheapOnly = binding.chipCheap.isChecked();
        boolean apartmentOnly = binding.chipApartment.isChecked();

        List<House> filtered = new ArrayList<>();
        for (House house : allHouses) {
            boolean matchesQuery = query.isEmpty() ||
                    house.getTitle().toLowerCase().contains(query) ||
                    house.getAddress().toLowerCase().contains(query);

            boolean matchesVerified = !verifiedOnly || house.isVerified();
            boolean matchesCheap = !cheapOnly || house.getPrice() <= 3.5;
            boolean matchesApartment = !apartmentOnly || "Căn hộ".equalsIgnoreCase(house.getHouseType());

            if (matchesQuery && matchesVerified && matchesCheap && matchesApartment) {
                filtered.add(house);
            }
        }

        if (filtered.isEmpty()) {
            binding.layoutResults.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.tvEmptyMsg.setText("Không tìm thấy nhà ở phù hợp");
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.layoutResults.setVisibility(View.VISIBLE);
            binding.tvResultCount.setText(filtered.size() + " kết quả tìm thấy");
            houseAdapter.submitList(filtered);
        }
    }
}
