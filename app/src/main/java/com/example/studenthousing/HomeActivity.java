package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.local.DatabaseSeeder;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.databinding.ActivityHomeBinding;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/** Màn hình trang chủ (home). */
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HouseAdapter houseAdapter;
    private PostRepository postRepository;
    private SessionManager session;
    private UserDao userDao;
    private List<House> allHouses = new ArrayList<>();
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        userDao = new UserDao(this);
        DatabaseSeeder.seedIfEmpty(this);
        postRepository = new PostRepository(this);

        setupHeader();
        setupHouseList();
        setupBottomNavigation();
        setupListeners();

        loadPostsFromRepository();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeaderUserInfo();
        if (binding.bottomNav != null && binding.bottomNav.bottomNav != null) {
            binding.bottomNav.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void setupHeader() {
        updateHeaderUserInfo();
    }

    private void updateHeaderUserInfo() {
        if (session != null && session.isLoggedIn()) {
            String familyName = session.getUserFamilyName();
            if (familyName != null && !familyName.isEmpty()) {
                binding.tvHomeUserName.setText(familyName);
                binding.tvHomeAvatar.setText(String.valueOf(familyName.charAt(0)).toUpperCase());
            } else {
                binding.tvHomeUserName.setText("Hộ gia đình");
                binding.tvHomeAvatar.setText("H");
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                User u = userDao.getById(session.getUserId());
                boolean isVerified = (u != null && (u.isResidenceVerified() || u.isVerified()));
                runOnUiThread(() -> {
                    if (binding.tvKycBadge != null) {
                        binding.tvKycBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                    }
                });
            });
        }
    }

    private void loadPostsFromRepository() {
        if (binding.swipeRefreshHome != null) {
            binding.swipeRefreshHome.setRefreshing(true);
        }
        postRepository.getLatestPosts(10, new AppCallback<List<AccommodationPost>>() {
            @Override
            public void onSuccess(List<AccommodationPost> posts) {
                runOnUiThread(() -> {
                    if (binding.swipeRefreshHome != null) {
                        binding.swipeRefreshHome.setRefreshing(false);
                    }
                    if (posts != null && !posts.isEmpty()) {
                        allHouses = convertPostsToHouses(posts);
                    } else {
                        allHouses = createSampleHouses();
                    }
                    applySearchFilter();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (binding.swipeRefreshHome != null) {
                        binding.swipeRefreshHome.setRefreshing(false);
                    }
                    allHouses = createSampleHouses();
                    applySearchFilter();
                });
            }
        });
    }

    private List<House> convertPostsToHouses(List<AccommodationPost> posts) {
        List<House> list = new ArrayList<>();
        int[] drawables = {R.drawable.house_thao_dien, R.drawable.home_linh_trung, R.drawable.house_linh_xuan, R.drawable.home_binh_thanh};
        int idx = 0;
        for (AccommodationPost p : posts) {
            int imageRes = drawables[idx % drawables.length];
            idx++;
            String host = p.getFamilyName() != null && !p.getFamilyName().isEmpty() ? p.getFamilyName() : "Hộ gia đình Nguyễn Văn A";
            String title = p.getTitle() != null ? p.getTitle() : "Nhà ở sinh viên";
            String addr = p.getAddress() != null ? p.getAddress() : "TP. Hồ Chí Minh";
            double dist = p.getDistanceKm() > 0 ? p.getDistanceKm() : 1.5;
            double price = p.getPrice() > 0 ? p.getPrice() : 3.5;
            double area = p.getArea() > 0 ? p.getArea() : 45.0;
            String desc = p.getDescription() != null && !p.getDescription().isEmpty() ? p.getDescription() : "Căn hộ rộng rãi, thoáng mát, gần trường học, an ninh tốt.";
            String type = p.getHouseType() != null ? p.getHouseType() : "Căn hộ";

            list.add(new House(
                    p.getPostId() != null ? p.getPostId() : "post_" + idx,
                    title,
                    addr,
                    dist,
                    p.isPoliceVerified(),
                    imageRes,
                    price,
                    area,
                    desc,
                    host,
                    type
            ));
        }
        return list;
    }

    private List<House> createSampleHouses() {
        return Arrays.asList(
                new House(
                        "house_1",
                        "Căn hộ cao cấp Thảo Điền",
                        "Phường Thảo Điền, Quận 2, TP. Thủ Đức",
                        1.2,
                        true,
                        R.drawable.house_thao_dien,
                        5.0,
                        65.0,
                        "Căn hộ cao cấp đầy đủ tiện nghi, view đẹp, bảo vệ 24/7, gần bến xe buýt và siêu thị.",
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
                        "Phòng trọ rộng rãi có gác lửng, toilet riêng, giờ giấc tự do, cách ĐHQG 500m.",
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
                        "Nhà nguyên căn 1 trệt 1 lầu, 2 phòng ngủ, bếp rộng, thích hợp cho nhóm 4-6 sinh viên ở ghép.",
                        "Hộ gia đình Phạm Hoàng",
                        "Nhà nguyên căn"
                ),
                new House(
                        "house_4",
                        "Căn hộ mini Bình Thạnh",
                        "Phường 25, Quận Bình Thạnh, TP.HCM",
                        2.5,
                        true,
                        R.drawable.home_binh_thanh,
                        3.8,
                        35.0,
                        "Phòng đẹp mới xây, có thang máy, trang bị máy lạnh, tủ lạnh, bếp từ.",
                        "Hộ gia đình Nguyễn Văn An",
                        "Căn hộ"
                )
        );
    }

    private void setupHouseList() {
        houseAdapter = new HouseAdapter(this::openHouseDetail);
        binding.rvHouses.setAdapter(houseAdapter);
    }

    private void applySearchFilter() {
        List<House> filtered = new ArrayList<>();
        for (House house : allHouses) {
            if (currentSearchQuery.isEmpty()
                    || house.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())
                    || house.getAddress().toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                filtered.add(house);
            }
        }

        if (filtered.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvHouses.setVisibility(View.GONE);
            binding.tvEmptyTitle.setText("Không tìm thấy chỗ ở");
            binding.tvEmptyDesc.setText("Thử tìm kiếm với từ khóa khác hoặc xóa bộ lọc.");
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvHouses.setVisibility(View.VISIBLE);
            houseAdapter.submitList(filtered);
        }
    }

    private void setupBottomNavigation() {
        if (binding.bottomNav != null && binding.bottomNav.bottomNav != null) {
            binding.bottomNav.bottomNav.setSelectedItemId(R.id.nav_home);
            binding.bottomNav.bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    startActivity(new Intent(this, SavedHousesActivity.class));
                    finish();
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

    private void setupListeners() {
        if (binding.swipeRefreshHome != null) {
            binding.swipeRefreshHome.setOnRefreshListener(this::loadPostsFromRepository);
        }

        if (binding.btnNotifications != null) {
            binding.btnNotifications.setOnClickListener(v ->
                    startActivity(new Intent(this, ExchangeHistoryActivity.class)));
        }

        if (binding.btnFilter != null) {
            binding.btnFilter.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchFilterActivity.class)));
        }

        if (binding.btnSearch != null) {
            binding.btnSearch.setOnClickListener(v -> showSearchDialog());
        }

        if (binding.efabCreatePostHome != null) {
            binding.efabCreatePostHome.setOnClickListener(v ->
                    startActivity(new Intent(this, MyPostsActivity.class)));
        }

        if (binding.btnEmptyAction != null) {
            binding.btnEmptyAction.setOnClickListener(v -> {
                currentSearchQuery = "";
                if (binding.tvSearchHint != null) {
                    binding.tvSearchHint.setText(R.string.home_search_hint);
                }
                applySearchFilter();
            });
        }

        if (binding.cardMatchSuggestion != null) {
            binding.cardMatchSuggestion.setOnClickListener(v ->
                    startActivity(new Intent(this, ExchangeHistoryActivity.class)));
        }
    }

    private void showSearchDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nhập tên, địa chỉ hoặc quận/huyện...");
        input.setText(currentSearchQuery);

        new AlertDialog.Builder(this)
                .setTitle("Tìm kiếm nhà ở")
                .setView(input)
                .setPositiveButton("Tìm kiếm", (dialog, which) -> {
                    currentSearchQuery = input.getText().toString().trim();
                    if (!currentSearchQuery.isEmpty()) {
                        binding.tvSearchHint.setText("Kết quả cho: \"" + currentSearchQuery + "\"");
                    } else {
                        binding.tvSearchHint.setText(R.string.home_search_hint);
                    }
                    applySearchFilter();
                })
                .setNegativeButton("Xóa tìm kiếm", (dialog, which) -> {
                    currentSearchQuery = "";
                    binding.tvSearchHint.setText(R.string.home_search_hint);
                    applySearchFilter();
                })
                .show();
    }

    private void openHouseDetail(House house) {
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
    }
}
