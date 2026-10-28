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
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.MatchSuggestion;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.databinding.ActivityHomeBinding;
import com.example.studenthousing.domain.FilterCriteria;
import com.example.studenthousing.ui.adapter.PostAdapter;
import com.example.studenthousing.ui.chat.ChatListActivity;
import com.example.studenthousing.ui.post.CreatePostActivity;
import com.example.studenthousing.ui.post.DetailActivity;
import com.example.studenthousing.ui.post.SavedHousesActivity;
import com.example.studenthousing.ui.profile.NotificationsActivity;
import com.example.studenthousing.ui.profile.ProfileActivity;
import com.example.studenthousing.ui.viewmodel.HomeViewModel;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
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

    private List<AccommodationPost> allPosts = new ArrayList<>();
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

        setupBottomNavigation();
        setupListeners();

        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            binding.swipeRefreshHome.setRefreshing(true);
            viewModel.refreshPosts();
            viewModel.refreshProfile(session.getUserId());
            loadMatchRequests(session.getUserId());
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
        loadMatchRequests(session.getUserId());
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
            updateMatchCard();
        });
        viewModel.getProfile().observe(this, state -> {
            if (state == null) return;
            User user = state.getContent();
            if (user != null) {
                String name = user.getFamilyName();
                binding.tvHomeUserName.setText((name == null || name.isEmpty()) ? "Bạn" : name);
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
        List<AccommodationPost> filtered = new ArrayList<>();
        for (AccommodationPost post : allPosts) {
            if (currentFilter.matches(post)) filtered.add(post);
        }
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

    private final List<MatchSuggestion> currentSuggestions = new ArrayList<>();
    private final java.util.Set<String> activePartnerPostIds = new java.util.HashSet<>();
    private final java.util.Set<String> settledMyPostIds = new java.util.HashSet<>();
    private boolean matchRequestsLoaded = false;

    private void loadMatchRequests(String userId) {
        matchRequestsLoaded = false;
        activePartnerPostIds.clear();
        settledMyPostIds.clear();
        if (userId == null || userId.isEmpty()) {
            matchRequestsLoaded = true;
            updateMatchCard();
            return;
        }
        new com.example.studenthousing.data.repository.ExchangeRepository(this).getAllForUser(userId,
                new AppCallback<>() {
                    @Override public void onSuccess(List<ExchangeRequest> requests) {
                        activePartnerPostIds.clear();
                        settledMyPostIds.clear();
                        if (requests != null) for (ExchangeRequest req : requests) {
                            String status = req.getStatus();
                            boolean isSettled = Constants.REQ_EXCHANGING.equalsIgnoreCase(status)
                                    || Constants.REQ_COMPLETED.equalsIgnoreCase(status);
                            boolean isActive = isSettled || "PENDING".equalsIgnoreCase(status);

                            if (userId.equals(req.getSenderId())) {
                                if (isActive && req.getReceiverPostId() != null) activePartnerPostIds.add(req.getReceiverPostId());
                                if (isSettled && req.getSenderPostId() != null) settledMyPostIds.add(req.getSenderPostId());
                            } else if (userId.equals(req.getReceiverId())) {
                                if (isActive && req.getSenderPostId() != null) activePartnerPostIds.add(req.getSenderPostId());
                                if (isSettled && req.getReceiverPostId() != null) settledMyPostIds.add(req.getReceiverPostId());
                            }
                        }
                        matchRequestsLoaded = true;
                        runOnUiThread(HomeActivity.this::updateMatchCard);
                    }
                    @Override public void onError(Exception e) {
                        matchRequestsLoaded = false;
                        currentSuggestions.clear();
                        runOnUiThread(() -> binding.cardMatchSuggestion.setVisibility(View.GONE));
                    }
                });
    }

    private void updateMatchCard() {
        String uid = session == null ? null : session.getUserId();
        if (uid == null || uid.isEmpty()) {
            binding.cardMatchSuggestion.setVisibility(View.GONE);
            return;
        }

        AccommodationPost mine = null;
        if (allPosts != null) {
            for (AccommodationPost p : allPosts) {
                if (p != null && uid.equals(p.getUserId()) && isAvailable(p)
                        && (!matchRequestsLoaded || !settledMyPostIds.contains(p.getPostId()))) {
                    mine = p;
                    break;
                }
            }
        }

        currentSuggestions.clear();
        if (mine != null && matchRequestsLoaded && allPosts != null) {
            // ✅ Đã bỏ điều kiện `uid == null` vì nếu mine != null thì uid chắc chắn != null
            for (AccommodationPost p : allPosts) {
                if (p == null || uid.equals(p.getUserId()) || !isAvailable(p)
                        || (p.getPostId() != null && activePartnerPostIds.contains(p.getPostId()))) continue;
                currentSuggestions.add(new MatchSuggestion(mine, p));
            }
        }

        if (binding == null) return;
        if (mine == null || currentSuggestions.isEmpty()) {
            binding.cardMatchSuggestion.setVisibility(View.GONE);
            return;
        }

        binding.cardMatchSuggestion.setVisibility(View.VISIBLE);
        binding.tvMatchSummary.setText("Có " + currentSuggestions.size() + " gợi ý đổi nhà");

        binding.cardMatchSuggestion.setOnClickListener(v -> {
            SuggestionsHolder.setSuggestions(HomeActivity.this, currentSuggestions);
            Intent intent = new Intent(HomeActivity.this, SuggestionsActivity.class);
            startActivity(intent);
        });
    }

    private static boolean isAvailable(AccommodationPost post) {
        return post != null && "AVAILABLE".equalsIgnoreCase(post.getStatus());
    }

    private void setupHouseList() {
        binding.rvHouses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new PostAdapter(new ArrayList<>(), this::openHouseDetail);
        binding.rvHouses.setAdapter(adapter);
    }

    private void updateMessagesBadge() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        com.google.android.material.badge.BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_messages);
        String userId = session == null ? null : session.getUserId();
        if (userId == null || userId.isEmpty()) {
            badge.setVisible(false);
            return;
        }
        new com.example.studenthousing.data.repository.ChatRepository(this).getUserRooms(userId,
                new AppCallback<>() {
                    @Override public void onSuccess(List<ChatRoom> rooms) {
                        runOnUiThread(() -> {
                            int count = 0;
                            if (rooms != null) for (ChatRoom room : rooms) {
                                if (room != null && room.getLastMessageTime() > 0
                                        && room.getLastSenderId() != null
                                        && !userId.equals(room.getLastSenderId())) count++;
                            }
                            badge.setVisible(count > 0);
                            if (count > 0) badge.setNumber(count);
                        });
                    }
                    @Override public void onError(Exception error) { runOnUiThread(() -> badge.setVisible(false)); }
                });
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
            if (id == R.id.nav_messages) { navigateTo(ChatListActivity.class); return true; }
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

    private void openHouseDetail(@NonNull AccommodationPost post) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        intent.putExtra("POST_ID", post.getPostId());
        intent.putExtra("house_title", post.getTitle());
        intent.putExtra("house_address", post.getAddress());
        startActivity(intent);
    }
}