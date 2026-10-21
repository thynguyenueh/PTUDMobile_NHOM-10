package com.example.studenthousing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SavedHousesActivity extends AppCompatActivity {

    private RecyclerView rvSavedHouses;
    private TextView tvTotalSaved;
    private HouseAdapter houseAdapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_houses);

        prefs = getSharedPreferences("saved_houses_prefs", Context.MODE_PRIVATE);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvSavedHouses = findViewById(R.id.rvSavedHouses);
        tvTotalSaved = findViewById(R.id.tvTotalSaved);

        setupRecyclerView();
        displaySavedHouses();
        setupBottomNav();
    }

    private void setupRecyclerView() {
        if (rvSavedHouses != null) {
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
            rvSavedHouses.setLayoutManager(new LinearLayoutManager(this));
            rvSavedHouses.setAdapter(houseAdapter);
        }
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_saved);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    startActivity(new Intent(this, ChatActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void displaySavedHouses() {
        List<House> savedList = new ArrayList<>();

        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("fav_") && Boolean.TRUE.equals(entry.getValue())) {
                String houseId = entry.getKey().substring(4);
                String title = prefs.getString("title_" + houseId, "Căn hộ cao cấp Thảo Điền");
                String address = prefs.getString("address_" + houseId, "Phường Thảo Điền, Quận 2, TP. Thủ Đức");

                savedList.add(new House(
                        houseId,
                        title,
                        address,
                        1.2,
                        true,
                        R.drawable.house_thao_dien,
                        5.0,
                        65.0,
                        "Căn hộ cao cấp đầy đủ tiện nghi",
                        "Hộ gia đình Lê Văn Nam",
                        "Căn hộ"
                ));
            }
        }

        if (savedList.isEmpty()) {
            savedList = Arrays.asList(
                    new House(
                            "house_saved_1",
                            "Căn hộ cao cấp Thảo Điền",
                            "Phường Thảo Điền, Quận 2, TP. Thủ Đức",
                            1.2,
                            true,
                            R.drawable.house_thao_dien,
                            5.0,
                            65.0,
                            "Căn hộ cao cấp đầy đủ tiện nghi",
                            "Hộ gia đình Lê Văn Nam",
                            "Căn hộ"
                    ),
                    new House(
                            "house_saved_2",
                            "Nhà nguyên căn Linh Xuân",
                            "Phường Linh Xuân, TP. Thủ Đức",
                            1.8,
                            true,
                            R.drawable.house_linh_xuan,
                            3.2,
                            45.0,
                            "Nhà nguyên căn 1 trệt 1 lầu",
                            "Hộ gia đình Phạm Hoàng",
                            "Nhà nguyên căn"
                    )
            );
        }

        if (tvTotalSaved != null) {
            tvTotalSaved.setText(savedList.size() + " căn nhà đã lưu");
        }

        if (houseAdapter != null) {
            houseAdapter.submitList(savedList);
        }
    }
}
