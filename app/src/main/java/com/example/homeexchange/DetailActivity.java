package com.example.homeexchange;

import com.example.studenthousing.R;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivityDetailBinding;
import com.example.studenthousing.databinding.ItemAmenityBinding;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

/** Màn hình chi tiết nhà (detail). */
public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPhotoPager();
        setupAmenities();
        setupListeners();
    }

    private void setupPhotoPager() {
        // TODO: thay bằng ảnh thật của căn hộ khi merge dữ liệu.
        List<Integer> photos = Arrays.asList(
                R.drawable.home_living_room,
                R.drawable.home_kitchen,
                R.drawable.home_bedroom);

        binding.photoPager.setAdapter(new PhotoPagerAdapter(photos));
        new TabLayoutMediator(binding.photoIndicator, binding.photoPager, (tab, position) -> { })
                .attach();
    }

    private void setupAmenities() {
        List<Amenity> amenities = Arrays.asList(
                new Amenity(R.drawable.ic_wifi, R.string.amenity_wifi),
                new Amenity(R.drawable.ic_air_conditioner, R.string.amenity_air_conditioner),
                new Amenity(R.drawable.ic_kitchen, R.string.amenity_kitchen),
                new Amenity(R.drawable.ic_parking_car, R.string.amenity_parking));

        for (Amenity amenity : amenities) {
            ItemAmenityBinding item = ItemAmenityBinding.inflate(
                    getLayoutInflater(), binding.amenityContainer, false);
            item.ivAmenityIcon.setImageResource(amenity.getIconRes());
            item.tvAmenityLabel.setText(amenity.getLabelRes());
            binding.amenityContainer.addView(item.getRoot());
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
        binding.btnChat.setOnClickListener(v -> openChat());
        binding.btnProposeSwap.setOnClickListener(v ->
                ProposalActivity.start(this, getString(R.string.detail_host_name)));
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        binding.btnFavorite.setImageResource(
                isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void openChat() {
        // TODO: điều hướng sang màn chat khi merge.
        Toast.makeText(this,
                getString(R.string.detail_chat_with, getString(R.string.detail_host_name)),
                Toast.LENGTH_SHORT).show();
    }
}
