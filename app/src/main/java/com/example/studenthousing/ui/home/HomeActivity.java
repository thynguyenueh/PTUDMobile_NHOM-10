package com.example.studenthousing.ui.home;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.databinding.ActivityHomeBinding;
import com.example.studenthousing.data.model.FilterCriteria;
import com.example.studenthousing.ui.adapter.PostAdapter;
import com.example.studenthousing.ui.post.CreatePostActivity;
import com.example.studenthousing.ui.post.DetailActivity;
import com.example.studenthousing.ui.post.MyPostsActivity;
import com.example.studenthousing.ui.post.SavedHousesActivity;
import com.example.studenthousing.ui.profile.NotificationsActivity;
import com.example.studenthousing.ui.profile.ProfileActivity;
import com.example.studenthousing.ui.viewmodel.HomeViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    // Đã bỏ LOADING vì không dùng — loading dùng SwipeRefresh thay vì state
    private enum State { CONTENT, EMPTY, ERROR }

    private ActivityHomeBinding binding;
    private PostAdapter adapter;
    private HomeViewModel viewModel;
    private SessionManager session;
    private boolean userVerified;

    private List<Post> allPosts = new ArrayList<>();
    private FilterCriteria currentFilter = new FilterCriteria();
    private boolean filterActive = false;
    private Runnable emptyAction;

    private final ActivityResultLauncher<Intent> filterLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                FilterCriteria criteria = IntentCompat.getSerializableExtra(
                        result.getData(), FilterCriteria.EXTRA, FilterCriteria.class);
                if (criteria != null) {
                    currentFilter = criteria;
                    filterActive = true;
                    applyFilter();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // ✅ bindHeader() CHỈ gọi 1 lần — không gọi lại ở onResume để tránh reset UI
        bindHeader();
        setupHouseList();
        observeViewModel();
        viewModel.loadPosts();

        setupBottomNavigation();
        setupListeners();

        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            binding.swipeRefreshHome.setRefreshing(true);
            viewModel.refreshPosts();
            viewModel.refreshProfile(session.getUserId());
        });
        binding.swipeRefreshHome.setColorSchemeResources(R.color.primary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ❌ KHÔNG gọi bindHeader() ở đây (sẽ reset tên/avatar)
        // ✅ Dùng refresh* để buộc load lại từ SQLite + Firestore mỗi lần quay về Home
        viewModel.refreshProfile(session.getUserId());
        viewModel.refreshPosts();
        updateMessagesBadge();
        try {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
            if (nav != null) {
                nav.setSelectedItemId(R.id.nav_home);
            }
        } catch (Throwable ignored) {}
    }

    private void observeViewModel() {
        viewModel.getPosts().observe(this, state -> {
            if (state == null) return;
            if (state.getScreenState() == UiState.ScreenState.LOADING) {
                binding.swipeRefreshHome.setRefreshing(true);
                return;
            }
            binding.swipeRefreshHome.setRefreshing(false);
            if (state.getScreenState() == UiState.ScreenState.ERROR) {
                if (allPosts.isEmpty()) showState(State.ERROR);
                return;
            }
            allPosts = state.getContent() == null ? new ArrayList<>() : state.getContent();
            applyFilter();
        });
        viewModel.getProfile().observe(this, state -> {
            if (state == null) return;
            User user = state.getContent();
            String sessionName = session.getUserFamilyName();
            String name = (user != null && user.getFamilyName() != null && !user.getFamilyName().isEmpty())
                    ? user.getFamilyName()
                    : (sessionName != null && !sessionName.isEmpty() ? sessionName : "Người dùng");
            binding.tvHomeUserName.setText(name);
            if (binding.tvHomeAvatar != null) {
                binding.tvHomeAvatar.setText(name.isEmpty() ? "N" : name.substring(0, 1).toUpperCase());
            }
            if (user != null) {
                userVerified = user.isVerified();
                binding.tvKycBadge.setVisibility(userVerified ? View.VISIBLE : View.GONE);
                ImageView avatar = binding.getRoot().findViewById(R.id.ivHomeAvatarImg);
                if (avatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    avatar.setVisibility(View.VISIBLE);
                    Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(avatar);
                    binding.tvHomeAvatar.setVisibility(View.GONE);
                }
            } else if (state.getScreenState() == UiState.ScreenState.ERROR) {
                binding.tvKycBadge.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Chỉ gọi 1 lần trong onCreate để set placeholder ban đầu.
     * Observer sẽ ghi đè bằng dữ liệu thật sau khi loadProfile xong.
     * KHÔNG gọi trong onResume — sẽ làm mất tên/avatar đã hiển thị.
     */
    private void bindHeader() {
        binding.tvHomeUserName.setText("Bạn");
        ImageView avatar = binding.getRoot().findViewById(R.id.ivHomeAvatarImg);
        if (avatar != null) avatar.setVisibility(View.GONE);
        binding.tvHomeAvatar.setVisibility(View.VISIBLE);
        binding.tvHomeAvatar.setText("B");
        binding.tvKycBadge.setVisibility(View.GONE);

        TextView tvNotifBadgeCount = binding.getRoot().findViewById(R.id.tvNotifBadgeCount);
        if (tvNotifBadgeCount != null) tvNotifBadgeCount.setVisibility(View.GONE);
    }

    private void loadPosts() {
        if (viewModel != null) viewModel.refreshPosts();
    }

    private void applyFilter() {
        filterActive = currentFilter.isActive();

        List<Post> filtered = new ArrayList<>();
        for (Post post : allPosts) {
            if (!currentFilter.matches(post)) continue;
            filtered.add(post);
        }

        // Sort posts from newest to oldest (createdAt descending)
        filtered.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

        adapter.updateData(filtered);
        showState(filtered.isEmpty() ? State.EMPTY : State.CONTENT);
        boolean trulyNoPosts = allPosts.isEmpty() && !filterActive;
        binding.efabCreatePostHome.setVisibility(trulyNoPosts ? View.GONE : View.VISIBLE);
    }

    private void clearFilter() {
        currentFilter = new FilterCriteria();
        filterActive = false;
        applyFilter();
    }

    private void showState(State state) {
        boolean showEmpty = state == State.EMPTY || state == State.ERROR;
        binding.layoutEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        binding.rvHouses.setVisibility(showEmpty ? View.GONE : View.VISIBLE);

        if (state == State.ERROR) {
            setEmpty("Không tải được dữ liệu", "Kiểm tra kết nối mạng rồi thử lại.", "Thử lại", this::loadPosts);
        } else if (state == State.EMPTY) {
            if (filterActive) {
                setEmpty("Không có nhà phù hợp", "Thử nới lỏng bộ lọc để xem thêm kết quả.",
                        "Xóa bộ lọc", this::clearFilter);
            } else {
                setEmpty("Chưa có bài đăng nào", "Hãy là người đầu tiên đăng nhà của bạn.", "Đăng bài",
                        () -> startActivity(new Intent(this, CreatePostActivity.class)));
            }
        }
    }

    private void setEmpty(String title, String desc, String action, Runnable onAction) {
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptyDesc.setText(desc);
        binding.btnEmptyAction.setText(action);
        emptyAction = onAction;
    }

    private void setupHouseList() {
        binding.rvHouses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new PostAdapter(new ArrayList<>(), this::openHouseDetail);
        binding.rvHouses.setAdapter(adapter);
    }

    private void updateMessagesBadge() {
        // Chat replaced with Manage Posts
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        if (target == HomeActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, 0, 0);
        startActivity(intent, options.toBundle());
        finish();
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_saved) { navigateTo(SavedHousesActivity.class); return true; }
            if (id == R.id.nav_manage_posts) { navigateTo(MyPostsActivity.class); return true; }
            if (id == R.id.nav_profile) { navigateTo(ProfileActivity.class); return true; }
            return false;
        });
    }

    private void openFilterScreen() {
        Intent intent = new Intent(this, SearchFilterActivity.class);
        intent.putExtra(FilterCriteria.EXTRA, currentFilter);
        filterLauncher.launch(intent);
    }

    private void setupListeners() {
        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        binding.btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        binding.btnFilter.setOnClickListener(v -> openFilterScreen());

        binding.btnEmptyAction.setOnClickListener(v -> { if (emptyAction != null) emptyAction.run(); });

        binding.tvHomeAvatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.efabCreatePostHome.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
    }

    private void openHouseDetail(@NonNull Post post) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        intent.putExtra("POST_ID", post.getPostId());
        intent.putExtra("house_title", post.getTitle());
        intent.putExtra("house_address", post.getAddress());
        startActivity(intent);
    }
}
