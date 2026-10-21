package com.example.studenthousing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studenthousing.databinding.ActivityDetailBinding;
import com.example.studenthousing.databinding.ItemAmenityBinding;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Màn hình chi tiết nhà (detail). */
public class DetailActivity extends AppCompatActivity {

    private static final String PREFS_FAVORITES = "saved_houses_prefs";

    private ActivityDetailBinding binding;
    private SharedPreferences prefs;

    private String houseId;
    private String houseTitle;
    private String houseAddress;
    private boolean houseVerified;
    private double houseDistance;
    private double housePrice;
    private double houseArea;
    private String houseDescription;
    private String houseHostName;
    private String houseType;
    private int housePhotoRes;

    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREFS_FAVORITES, Context.MODE_PRIVATE);

        parseIntentData();
        setupUI();
        setupPhotoPager();
        setupAmenities();
        setupFavoriteState();
        setupListeners();
    }

    private void parseIntentData() {
        Intent intent = getIntent();
        houseId = intent.getStringExtra("house_id");
        if (houseId == null || houseId.isEmpty()) {
            houseId = "house_default_" + System.currentTimeMillis();
        }

        houseTitle = intent.getStringExtra("house_title");
        if (houseTitle == null || houseTitle.isEmpty()) {
            houseTitle = getString(R.string.detail_title);
        }

        houseAddress = intent.getStringExtra("house_address");
        if (houseAddress == null || houseAddress.isEmpty()) {
            houseAddress = "Phường Thảo Điền, Quận 2, TP. Hồ Chí Minh";
        }

        houseVerified = intent.getBooleanExtra("house_verified", true);
        houseDistance = intent.getDoubleExtra("house_distance", 1.2);
        housePrice = intent.getDoubleExtra("house_price", 3.5);
        houseArea = intent.getDoubleExtra("house_area", 45.0);

        houseDescription = intent.getStringExtra("house_description");
        if (houseDescription == null || houseDescription.isEmpty()) {
            houseDescription = "Căn hộ rộng rãi, thoáng mát, đầy đủ tiện nghi, phù hợp cho sinh viên và hộ gia đình nhỏ. Môi trường an ninh, yên tĩnh, thuận tiện đi lại đến các trường đại học.";
        }

        houseHostName = intent.getStringExtra("house_host_name");
        if (houseHostName == null || houseHostName.isEmpty()) {
            houseHostName = getString(R.string.detail_host_name);
        }

        houseType = intent.getStringExtra("house_type");
        if (houseType == null || houseType.isEmpty()) {
            houseType = "Căn hộ";
        }

        housePhotoRes = intent.getIntExtra("house_photo_res", R.drawable.house_thao_dien);
    }

    private void setupUI() {
        binding.tvTitle.setText(houseTitle);
        binding.tvLocation.setText(houseAddress);

        String priceStr = housePrice + " Tr";
        binding.tvPriceBig.setText(priceStr);

        TextView tvBottomPrice = findViewById(R.id.tvBottomPrice);
        if (tvBottomPrice != null) {
            tvBottomPrice.setText(priceStr + " /tháng");
        }

        binding.tvHouseType.setText(houseType);
        binding.tvAreaMeta.setText("Diện tích sử dụng: " + houseArea + " m² · Đã qua thẩm định");
        binding.tvDescription.setText(houseDescription);

        if (binding.tvVerifiedBadge != null) {
            binding.tvVerifiedBadge.setVisibility(houseVerified ? View.VISIBLE : View.GONE);
        }

        // Host card bindings inside layout_host_card
        TextView tvHostName = findViewById(R.id.tvHostName);
        if (tvHostName != null) {
            tvHostName.setText(houseHostName);
        }

        View tvHostVerified = findViewById(R.id.tvHostVerified);
        if (tvHostVerified != null) {
            tvHostVerified.setVisibility(houseVerified ? View.VISIBLE : View.GONE);
        }

        TextView tvHostRating = findViewById(R.id.tvHostRating);
        if (tvHostRating != null) {
            tvHostRating.setText("4.9");
        }

        // Distance card bindings inside layout_distance_card
        TextView tvDistanceValue = findViewById(R.id.tvDistanceValue);
        if (tvDistanceValue != null) {
            tvDistanceValue.setText(houseDistance + " km · ĐHQG TP.HCM");
        }
    }

    private void setupPhotoPager() {
        List<Integer> photos = new ArrayList<>();
        photos.add(housePhotoRes);
        photos.add(R.drawable.home_living_room);
        photos.add(R.drawable.home_kitchen);
        photos.add(R.drawable.home_bedroom);

        binding.photoPager.setAdapter(new PhotoPagerAdapter(photos));

        new TabLayoutMediator(binding.photoIndicator, binding.photoPager, (tab, position) -> { }).attach();

        binding.photoPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tvPhotoCounter.setText((position + 1) + " / " + photos.size());
            }
        });
    }

    private void setupAmenities() {
        List<Amenity> amenities = Arrays.asList(
                new Amenity(R.drawable.ic_wifi, R.string.create_post_util_wifi),
                new Amenity(R.drawable.ic_air_conditioner, R.string.create_post_util_ac),
                new Amenity(R.drawable.ic_kitchen, R.string.create_post_util_kitchen),
                new Amenity(R.drawable.ic_parking_car, R.string.create_post_util_parking));

        binding.gridAmenities.removeAllViews();
        for (Amenity amenity : amenities) {
            ItemAmenityBinding item = ItemAmenityBinding.inflate(
                    getLayoutInflater(), binding.gridAmenities, false);
            item.ivAmenityIcon.setImageResource(amenity.getIconRes());
            item.tvAmenityLabel.setText(amenity.getLabelRes());
            binding.gridAmenities.addView(item.getRoot());
        }
    }

    private void setupFavoriteState() {
        isFavorite = prefs.getBoolean("fav_" + houseId, false);
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        binding.btnFavorite.setImageResource(
                isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        prefs.edit().putBoolean("fav_" + houseId, isFavorite)
                .putString("title_" + houseId, houseTitle)
                .putString("price_area_" + houseId, housePrice + " Tr/tháng · " + houseArea + " m²")
                .putString("address_" + houseId, houseAddress + " (" + houseDistance + " km)")
                .apply();

        updateFavoriteIcon();
        Toast.makeText(this,
                isFavorite ? "Đã lưu vào danh sách yêu thích" : "Đã bỏ lưu",
                Toast.LENGTH_SHORT).show();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());

        View btnChat = findViewById(R.id.btnChat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> openChat());
        }

        View btnProposeSwap = findViewById(R.id.btnProposeSwap);
        if (btnProposeSwap != null) {
            btnProposeSwap.setOnClickListener(v -> openProposal());
        }
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("partner_name", houseHostName);
        intent.putExtra("house_title", houseTitle);
        startActivity(intent);
    }

    private void openProposal() {
        ProposalActivity.start(
                this,
                houseHostName,
                houseId,
                houseTitle,
                houseArea,
                housePhotoRes
        );
    }
}
