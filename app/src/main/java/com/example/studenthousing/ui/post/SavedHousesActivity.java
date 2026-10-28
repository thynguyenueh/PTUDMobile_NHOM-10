package com.example.studenthousing.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.databinding.ActivitySavedHousesBinding;
import com.example.studenthousing.ui.adapter.SavedHouseAdapter;
import com.example.studenthousing.ui.chat.ChatListActivity;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.ui.profile.ProfileActivity;
import com.example.studenthousing.ui.viewmodel.SavedHousesViewModel;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class SavedHousesActivity extends AppCompatActivity implements SavedHouseAdapter.OnSavedHouseActionListener {

    private ActivitySavedHousesBinding binding;
    private SavedHouseAdapter savedHouseAdapter;
    private SavedHousesViewModel viewModel;
    private boolean retrySavedLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedHousesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SavedHousesViewModel.class);

        binding.rvSavedHouses.setLayoutManager(new LinearLayoutManager(this));
        savedHouseAdapter = new SavedHouseAdapter(this);
        binding.rvSavedHouses.setAdapter(savedHouseAdapter);

        viewModel.getSavedPosts().observe(this, this::renderSavedPosts);
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedHouses();
        updateMessagesBadge();
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        if (targetActivity == HomeActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        android.app.ActivityOptions options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0);
        startActivity(intent, options.toBundle());
        finish();
    }

    private void updateMessagesBadge() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        com.google.android.material.badge.BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_messages);
        String userId = new com.example.studenthousing.util.SessionManager(this).getUserId();
        new com.example.studenthousing.data.repository.ChatRepository(this).getUserRooms(userId,
                new AppCallback<List<com.example.studenthousing.data.model.ChatRoom>>() {
                    @Override public void onSuccess(List<com.example.studenthousing.data.model.ChatRoom> rooms) {
                        runOnUiThread(() -> {
                            int count = 0;
                            if (rooms != null) for (com.example.studenthousing.data.model.ChatRoom room : rooms) {
                                if (room != null && room.getLastMessageTime() > 0 && room.getLastSenderId() != null
                                        && !userId.equals(room.getLastSenderId())) count++;
                            }
                            badge.setVisible(count > 0);
                            if (count > 0) badge.setNumber(count);
                        });
                    }
                    @Override public void onError(Exception error) { runOnUiThread(() -> badge.setVisible(false)); }
                });
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_saved);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    navigateTo(HomeActivity.class);
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    navigateTo(ChatListActivity.class);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    navigateTo(ProfileActivity.class);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadSavedHouses() {
        if (viewModel != null) viewModel.loadSavedPosts();
    }

    private void renderSavedPosts(UiState<List<AccommodationPost>> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.LOADING) {
            binding.rvSavedHouses.setVisibility(android.view.View.GONE);
            return;
        }
        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            retrySavedLoad = true;
            savedHouseAdapter.submitList(new ArrayList<>());
            binding.rvSavedHouses.setVisibility(android.view.View.GONE);
            if (binding.tvSavedCount != null) binding.tvSavedCount.setText("0 căn nhà");
            return;
        }
        if (state.getContent() == null) return;

        retrySavedLoad = false;
        List<SavedHouseAdapter.SavedHouseItem> list = new ArrayList<>();
        for (AccommodationPost post : state.getContent()) {
            list.add(SavedHouseAdapter.SavedHouseItem.fromPost(this, post));
        }
        savedHouseAdapter.submitList(list);
        if (binding.tvSavedCount != null) binding.tvSavedCount.setText(list.size() + " căn nhà");
        boolean isEmpty = list.isEmpty();
        binding.rvSavedHouses.setVisibility(isEmpty ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    @Override
    public void onRemoveSaved(int position, @NonNull SavedHouseAdapter.SavedHouseItem item) {
        if (item.postId == null) {
            savedHouseAdapter.removeItem(position);
            int remaining = savedHouseAdapter.getItemCount();
            Toast.makeText(this, "Đã bỏ lưu '" + item.title + "'", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.unsavePost(item.postId, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    savedHouseAdapter.removeItem(position);
                    int remaining = savedHouseAdapter.getItemCount();
                    Toast.makeText(SavedHousesActivity.this, "Đã bỏ lưu '" + item.title + "'", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(SavedHousesActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onViewDetail(int position, @NonNull SavedHouseAdapter.SavedHouseItem item) {
        Intent intent = new Intent(SavedHousesActivity.this, DetailActivity.class);
        intent.putExtra("post_id", item.postId);
        intent.putExtra("POST_ID", item.postId);
        startActivity(intent);
    }
}
