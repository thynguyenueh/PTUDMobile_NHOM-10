package com.example.studenthousing.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studenthousing.R;
import com.example.studenthousing.data.loader.UtilitiesDataLoader;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.Utility;
import com.example.studenthousing.data.repository.CampusRepository;
import com.example.studenthousing.databinding.ActivityDetailBinding;
import com.example.studenthousing.ui.adapter.PhotoPagerAdapter;
import com.example.studenthousing.ui.chat.ChatActivity;
import com.example.studenthousing.ui.dialog.FullScreenImageDialog;
import com.example.studenthousing.ui.review.UserReviewsActivity;
import com.example.studenthousing.ui.viewmodel.DetailViewModel;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private DetailViewModel viewModel;
    private String postId;
    private AccommodationPost currentPost;
    private boolean isFavorite = false;

    private TextView tvTitle, tvLocation, tvHostName, tvHostInitial, tvDescription,
            tvPhotoCounter, tvDescriptionLabel, tvHostRating, tvHostReviewCount;
    private View tvHostVerified;
    private View cardHost, cardDistance, cardDescription, detailBottomBar;
    private View btnChat, btnCall;
    private ImageView ivStar;
    private GridLayout gridAmenities;
    private TextView tvAmenitiesEmpty;

    private ViewPager2.OnPageChangeCallback pageCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        postId = getIntent().getStringExtra("post_id");
        if (postId == null || postId.trim().isEmpty()) {
            postId = getIntent().getStringExtra("POST_ID");
        }
        AccommodationPost postExtra = getIntent().getParcelableExtra("post");
        if ((postId == null || postId.trim().isEmpty()) && postExtra != null) postId = postExtra.getPostId();
        if (postId == null || postId.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "Không tìm thấy mã bài đăng", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvHostName = findViewById(R.id.tvHostName);
        tvHostInitial = findViewById(R.id.tvHostInitial);
        tvDescription = findViewById(R.id.tvDescription);
        tvDescriptionLabel = findViewById(R.id.tvDescriptionLabel);
        tvPhotoCounter = findViewById(R.id.tvPhotoCounter);
        tvHostVerified = findViewById(R.id.tvHostVerified);
        tvHostRating = findViewById(R.id.tvHostRating);
        tvHostReviewCount = findViewById(R.id.tvHostReviewCount);
        cardHost = findViewById(R.id.cardHost);
        cardDistance = findViewById(R.id.cardDistance);
        cardDescription = findViewById(R.id.cardDescription);
        detailBottomBar = findViewById(R.id.detailBottomBar);
        btnChat = findViewById(R.id.btnChat);
        btnCall = findViewById(R.id.btnCall);
        ivStar = findViewById(R.id.ivStar);
        gridAmenities = findViewById(R.id.gridAmenities);
        tvAmenitiesEmpty = findViewById(R.id.tvAmenitiesEmpty);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnFavorite = findViewById(R.id.btnFavorite);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> toggleFavorite());

        if (cardHost != null) {
            cardHost.setOnClickListener(v -> {
                if (currentPost == null || currentPost.getUserId() == null || currentPost.getUserId().trim().isEmpty()) return;
                Intent intent = new Intent(DetailActivity.this, UserReviewsActivity.class);
                intent.putExtra(UserReviewsActivity.EXTRA_TARGET_USER_ID, currentPost.getUserId());
                intent.putExtra(UserReviewsActivity.EXTRA_TARGET_USER_NAME, currentPost.getFamilyName());
                startActivity(intent);
            });
        }

        setupPhotoPager(null);

        viewModel.getFavoriteState().observe(this, saved -> {
            isFavorite = Boolean.TRUE.equals(saved);
            binding.btnFavorite.setImageResource(
                    isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        });

        viewModel.getDetailState().observe(this, this::renderDetailState);

        if (postExtra != null && postExtra.getPostId() != null && postExtra.getPostId().equals(postId)) {
            renderPostImmediately(postExtra);
        }
        if (postId != null) {
            viewModel.loadPostDetail(postId);
        }

        btnChat = findViewById(R.id.btnChat);
        if (btnChat != null) btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ChatActivity.class);
            if (currentPost != null) {
                intent.putExtra("target_user_id", currentPost.getUserId());
                intent.putExtra("ROOM_NAME", currentPost.getFamilyName());
                intent.putExtra("POST_ID", currentPost.getPostId());
            }
            startActivity(intent);
        });
    }

    private void renderPostImmediately(AccommodationPost post) {
        if (post == null) return;
        currentPost = post;
        if (tvTitle != null && post.getTitle() != null) tvTitle.setText(post.getTitle());
        if (tvLocation != null) tvLocation.setText(formatPublicAddress(post, false));
        if (tvHostName != null && post.getFamilyName() != null) {
            tvHostName.setText(post.getFamilyName());
            if (tvHostInitial != null && !post.getFamilyName().trim().isEmpty()) {
                tvHostInitial.setText(post.getFamilyName().trim().substring(0, 1).toUpperCase());
            }
        }
        bindDescription(post.getDescription());
        setupAmenities(post.getUtilities());
        String currentUid = SessionManager.getUid(this);
        boolean isOwner = currentUid != null && currentUid.equals(post.getUserId());
        updateActionAvailability(isOwner, post);
        List<String> images = post.getImages();
        setupPhotoPager(images != null && !images.isEmpty() ? images : null);
        setupNearbyCampuses(post.getNearbyCampuses());
    }

    private String formatPublicAddress(AccommodationPost post, boolean showFull) {
        if (post == null) return "";
        if (showFull && post.getAddress() != null && !post.getAddress().trim().isEmpty()) {
            return post.getAddress();
        }
        String ward = post.getWardName() == null ? "" : post.getWardName().trim();
        String city = post.getCityName() == null ? "" : post.getCityName().trim();
        if (!ward.isEmpty() && !city.isEmpty()) return ward + ", " + city;
        if (!ward.isEmpty()) return ward;
        if (!city.isEmpty()) return city;
        return "Đang ẩn địa chỉ chi tiết";
    }

    private void bindDescription(String description) {
        boolean hasDescription = description != null && !description.trim().isEmpty();
        if (hasDescription && tvDescription != null) tvDescription.setText(description.trim());
        if (tvDescriptionLabel != null) {
            tvDescriptionLabel.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        }
        if (cardDescription != null) cardDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
    }

    private void updateActionAvailability(boolean isOwner, AccommodationPost post) {
        if (detailBottomBar != null) detailBottomBar.setVisibility(View.VISIBLE);

        if (isOwner) {
            if (btnChat != null && btnChat instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnChat).setText("Chỉnh sửa");
                btnChat.setVisibility(View.VISIBLE);
                btnChat.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CreatePostActivity.class);
                    intent.putExtra("IS_EDIT_MODE", true);
                    intent.putExtra("POST_ID", post.getPostId());
                    intent.putExtra("POST_TITLE", post.getTitle());
                    intent.putExtra("POST_ADDRESS", post.getAddress());
                    intent.putExtra("POST_DESC", post.getDescription());
                    intent.putExtra("POST_STREET_NAME", post.getStreetName());
                    intent.putExtra("POST_HOUSE_NUMBER", post.getHouseNumber());
                    intent.putExtra("POST_CITY_CODE", post.getCityCode());
                    intent.putExtra("POST_WARD_CODE", post.getWardCode());
                    intent.putExtra("POST_WARD_NAME", post.getWardName());
                    intent.putStringArrayListExtra("POST_UTILITIES",
                            post.getUtilities() != null ? new ArrayList<>(post.getUtilities()) : new ArrayList<>());
                    intent.putStringArrayListExtra("POST_IMAGES",
                            post.getImages() != null ? new ArrayList<>(post.getImages()) : new ArrayList<>());
                    startActivity(intent);
                });
            }
            if (btnCall != null) {
                btnCall.setVisibility(View.GONE);
            }
        } else {
            boolean available = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());
            if (btnChat != null && btnChat instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnChat).setText(R.string.action_message);
                btnChat.setVisibility(available ? View.VISIBLE : View.GONE);
                btnChat.setOnClickListener(v -> {
                    Intent intent = new Intent(DetailActivity.this, ChatActivity.class);
                    if (currentPost != null) {
                        intent.putExtra("target_user_id", currentPost.getUserId());
                        intent.putExtra("ROOM_NAME", currentPost.getFamilyName());
                        intent.putExtra("POST_ID", currentPost.getPostId());
                    }
                    startActivity(intent);
                });
            }
            if (btnCall != null) {
                btnCall.setVisibility(available ? View.VISIBLE : View.GONE);
                btnCall.setOnClickListener(v -> {
                    if (currentPost == null || currentPost.getUserId() == null || currentPost.getUserId().trim().isEmpty()) {
                        android.widget.Toast.makeText(DetailActivity.this, "Không có thông tin đối tác", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(DetailActivity.this);
                    com.example.studenthousing.data.model.User host = userDao.getById(currentPost.getUserId());
                    String phone = host != null ? host.getPhone() : null;
                    if (phone == null || phone.trim().isEmpty()) {
                        android.widget.Toast.makeText(DetailActivity.this,
                                "Đối tác chưa cung cấp số điện thoại", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent callIntent = new Intent(Intent.ACTION_DIAL,
                            android.net.Uri.fromParts("tel", phone.trim(), null));
                    if (callIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(callIntent);
                    } else {
                        android.widget.Toast.makeText(DetailActivity.this,
                                "Không tìm thấy ứng dụng gọi điện", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void renderDetailState(UiState<DetailViewModel.PostDetailData> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.ERROR && state.getContent() == null) {
            android.widget.Toast.makeText(this, "Lỗi tải bài đăng",
                    android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (state.getContent() == null) return;

        DetailViewModel.PostDetailData data = state.getContent();
        AccommodationPost post = data.post;
        currentPost = post;

        if (tvTitle != null && post.getTitle() != null) tvTitle.setText(post.getTitle());

        String currentUid = SessionManager.getUid(this);
        boolean isOwner = currentUid != null && post.getUserId() != null && currentUid.equals(post.getUserId());

        if (tvLocation != null) {
            tvLocation.setText(formatPublicAddress(post, isOwner));
        }
        updateActionAvailability(isOwner, post);
        if (tvHostName != null && post.getFamilyName() != null && !post.getFamilyName().isEmpty()) {
            tvHostName.setText(post.getFamilyName());
        }

        bindDescription(post.getDescription());
        setupNearbyCampuses(post.getNearbyCampuses());
        setupAmenities(post.getUtilities());

        List<String> images = post.getImages();
        if (images != null && !images.isEmpty()) {
            setupPhotoPager(images);
        } else {
            setupPhotoPager(null);
        }

        User user = data.host;
        if (user != null) {
            if (tvHostName != null && user.getFamilyName() != null && !user.getFamilyName().isEmpty()) {
                tvHostName.setText(user.getFamilyName());
                if (tvHostInitial != null && !user.getFamilyName().trim().isEmpty()) {
                    tvHostInitial.setText(user.getFamilyName().trim().substring(0, 1).toUpperCase());
                }
            }
            if (tvHostVerified != null) {
                tvHostVerified.setVisibility(user.isResidenceVerified() ? View.VISIBLE : View.GONE);
            }
        }

        List<Review> reviews = data.hostReviews;
        if (reviews == null || reviews.isEmpty()) {
            if (tvHostRating != null) tvHostRating.setText("Chưa có");
            if (tvHostReviewCount != null) tvHostReviewCount.setText("(0 đánh giá)");
            if (ivStar != null) ivStar.setVisibility(View.GONE);
        } else {
            double sum = 0;
            for (Review r : reviews) sum += r.getRating();
            double avg = sum / reviews.size();
            if (tvHostRating != null) {
                tvHostRating.setText(String.format(java.util.Locale.US, "%.1f", avg));
            }
            if (tvHostReviewCount != null) {
                tvHostReviewCount.setText(getString(R.string.detail_review_count_format, reviews.size()));
            }
            if (ivStar != null) ivStar.setVisibility(View.VISIBLE);
        }
    }

    private void setupNearbyCampuses(List<NearbyCampus> nearby) {
        View cardDist = findViewById(R.id.cardDistance);
        View tvNearbyLabel = findViewById(R.id.tvNearbyCampusesLabel);
        LinearLayout container = findViewById(R.id.containerNearbyUnis);
        if (cardDist == null) return;
        if (container != null) container.removeAllViews();

        if (nearby == null || nearby.isEmpty()) {
            cardDist.setVisibility(View.GONE);
            if (tvNearbyLabel != null) tvNearbyLabel.setVisibility(View.GONE);
            return;
        }

        List<NearbyCampus> sorted = new ArrayList<>(nearby);
        sorted.sort((a, b) -> Double.compare(a.getKm(), b.getKm()));
        int limit = Math.min(5, sorted.size());

        if (container != null) {
            for (int i = 0; i < limit; i++) {
                NearbyCampus campus = sorted.get(i);
                if (campus == null || campus.getCampusId() == null || campus.getKm() <= 0) continue;
                Campus campusModel = CampusRepository.getInstance(this).findByCode(campus.getCampusId());
                String name = campusModel == null ? campus.getCampusId() : campusModel.getDisplayName();
                String distance = String.format(java.util.Locale.getDefault(), "%.1f km", campus.getKm());
                TextView item = new TextView(this);
                item.setText("• Cách " + name + ": " + (campus.isEstimate() ? "~" + distance : distance));
                item.setTextColor(getColor(R.color.surface_card));
                item.setTextSize(13);
                item.setPadding(0, 4, 0, 4);
                container.addView(item);
            }
        }

        boolean hasItems = container != null && container.getChildCount() > 0;
        cardDist.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (tvNearbyLabel != null) tvNearbyLabel.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        View.OnClickListener openAll = v -> {
            Intent intent = new Intent(this, CampusDistanceDetailActivity.class);
            intent.putExtra("NEARBY_CAMPUSES", new ArrayList<>(sorted));
            startActivity(intent);
        };
        cardDist.setOnClickListener(openAll);
        View btnViewAll = findViewById(R.id.btnViewAllCampuses);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(openAll);
            btnViewAll.setVisibility(sorted.size() > 5 ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * FIX: Nhận vào danh sách CODE, dịch sang LABEL để hiển thị.
     */
    private void setupAmenities(List<String> utilities) {
        if (gridAmenities == null) return;
        gridAmenities.removeAllViews();
        if (utilities == null || utilities.isEmpty()) {
            if (tvAmenitiesEmpty != null) tvAmenitiesEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvAmenitiesEmpty != null) tvAmenitiesEmpty.setVisibility(View.GONE);

        for (String utilCode : utilities) {
            View item = getLayoutInflater().inflate(R.layout.item_amenity, gridAmenities, false);
            ImageView icon = item.findViewById(R.id.ivAmenityIcon);
            TextView label = item.findViewById(R.id.tvAmenityLabel);

            // Dịch code → label
            Utility u = UtilitiesDataLoader.getByCode(this, utilCode);
            String displayLabel = (u != null) ? u.getLabel() : utilCode;
            String iconName = (u != null) ? u.getIcon() : null;

            label.setText(displayLabel);
            icon.setImageResource(getIconForUtility(displayLabel, iconName));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            item.setLayoutParams(params);

            gridAmenities.addView(item);
        }
    }

    private int getIconForUtility(String u, String iconName) {
        if (iconName != null && !iconName.trim().isEmpty()) {
            switch (iconName.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "ic_wifi": return R.drawable.ic_wifi;
                case "ic_parking": return R.drawable.ic_parking_car;
                case "ic_ac": return R.drawable.ic_air_conditioner;
                case "ic_washer": return R.drawable.ic_washer;
                case "ic_fridge": return R.drawable.ic_kitchen;
                case "ic_bath": return R.drawable.ic_bath;
                case "ic_security": return R.drawable.ic_shield_check;
                case "ic_elevator": return R.drawable.ic_elevator;
                case "ic_pet": return R.drawable.ic_pet;
            }
        }
        if (u == null) return R.drawable.ic_check;
        String key = u.toLowerCase();
        if (key.contains("wifi") || key.contains("wi-fi")) return R.drawable.ic_wifi;
        if (key.contains("điều hòa") || key.contains("điều hoà") || key.contains("máy lạnh"))
            return R.drawable.ic_air_conditioner;
        if (key.contains("bếp")) return R.drawable.ic_kitchen;
        if (key.contains("đỗ xe") || key.contains("gửi xe") || key.contains("xe")) return R.drawable.ic_parking_car;
        if (key.contains("máy giặt") || key.contains("giặt")) return R.drawable.ic_washer;
        if (key.contains("vệ sinh") || key.contains("wc") || key.contains("tắm")) return R.drawable.ic_bath;
        if (key.contains("thang máy")) return R.drawable.ic_elevator;
        if (key.contains("thú cưng") || key.contains("chó") || key.contains("mèo") || key.contains("pet")) return R.drawable.ic_pet;
        if (key.contains("ban công")) return R.drawable.ic_location;
        if (key.contains("bảo vệ") || key.contains("an ninh") || key.contains("camera"))
            return R.drawable.ic_shield_check;
        if (key.contains("tủ lạnh")) return R.drawable.ic_kitchen;
        return R.drawable.ic_check;
    }

    private void setupPhotoPager(List<String> imageUrls) {
        List<String> safeUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        binding.photoPager.setAdapter(new PhotoPagerAdapter(safeUrls, position -> {
            showFullScreenPhoto(safeUrls, position);
        }));

        setupDotIndicator(safeUrls.size());

        if (pageCallback != null) {
            binding.photoPager.unregisterOnPageChangeCallback(pageCallback);
        }
        final int total = safeUrls.size();
        if (tvPhotoCounter != null) {
            tvPhotoCounter.setText(total > 0 ? "1 / " + total : "0 / 0");
        }

        if (total > 0) {
            pageCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateDotIndicator(position);
                    if (tvPhotoCounter != null) {
                        tvPhotoCounter.setText((position + 1) + " / " + total);
                    }
                }
            };
            binding.photoPager.registerOnPageChangeCallback(pageCallback);
        }
    }

    private void setupDotIndicator(int totalImages) {
        LinearLayout dots = findViewById(R.id.photoIndicator);
        if (dots == null) return;
        dots.removeAllViews();
        if (totalImages <= 0) return;

        int size = dp(8);
        int gap = dp(4);

        for (int i = 0; i < totalImages; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap, 0, gap, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.tab_dot_selector);
            dot.setSelected(i == 0);
            dots.addView(dot);
        }
    }

    private void updateDotIndicator(int position) {
        LinearLayout dots = findViewById(R.id.photoIndicator);
        if (dots == null) return;
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setSelected(i == position);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void showFullScreenPhoto(List<String> urls, int startPos) {
        FullScreenImageDialog.show(this, urls, startPos);
    }

    private void toggleFavorite() {
        viewModel.toggleFavorite(postId);
    }
}