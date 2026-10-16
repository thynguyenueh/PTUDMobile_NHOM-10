package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.local.DatabaseSeeder;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.databinding.ActivityHomeBinding;
import com.example.studenthousing.util.AppCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Màn hình trang chủ (home). */
public class HomeActivity extends AppCompatActivity {

    private static final double NEAR_DISTANCE_KM = 2.0;

    private ActivityHomeBinding binding;
    private HouseAdapter houseAdapter;
    private PostRepository postRepository;
    private List<House> allHouses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DatabaseSeeder.seedIfEmpty(this);
        postRepository = new PostRepository(this);

        setupHouseList();
        setupFilterChips();
        setupBottomNavigation();
        setupListeners();

        loadPostsFromRepository();
    }

    private void loadPostsFromRepository() {
        postRepository.getLatestPosts(10, new AppCallback<List<AccommodationPost>>() {
            @Override
            public void onSuccess(List<AccommodationPost> posts) {
                runOnUiThread(() -> {
                    if (posts != null && !posts.isEmpty()) {
                        allHouses = convertPostsToHouses(posts);
                    } else {
                        allHouses = createSampleHouses();
                    }
                    applyFilter(binding.chipGroupFilter.getCheckedChipId());
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    allHouses = createSampleHouses();
                    applyFilter(binding.chipGroupFilter.getCheckedChipId());
                });
            }
        });
    }

    private List<House> convertPostsToHouses(List<AccommodationPost> posts) {
        List<House> list = new ArrayList<>();
        int[] drawables = {R.drawable.house_thao_dien, R.drawable.home_linh_trung, R.drawable.house_linh_xuan};
        int idx = 0;
        for (AccommodationPost p : posts) {
            int imageRes = drawables[idx % drawables.length];
            idx++;
            list.add(new House(
                    p.getTitle() != null ? p.getTitle() : "Nhà ở sinh viên",
                    p.getAddress() != null ? p.getAddress() : "TP. Hồ Chí Minh",
                    p.getDistanceKm() > 0 ? p.getDistanceKm() : 1.5,
                    p.isPoliceVerified(),
                    imageRes
            ));
        }
        return list;
    }

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
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_saved) {
                startActivity(new Intent(this, SavedHousesActivity.class));
                return true;
            } else if (itemId == R.id.nav_messages) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        binding.btnNotifications.setOnClickListener(v -> showComingSoon());
        binding.btnFilter.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        binding.tvSeeAll.setOnClickListener(v -> showComingSoon());
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                showComingSoon();
                return true;
            }
            return false;
        });
    }

    private void openHouseDetail(@NonNull House house) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("house_title", house.getTitle());
        intent.putExtra("house_address", house.getAddress());
        intent.putExtra("house_verified", house.isVerified());
        startActivity(intent);
    }

    private void showComingSoon() {
        Toast.makeText(this, R.string.home_coming_soon, Toast.LENGTH_SHORT).show();
    }
}
