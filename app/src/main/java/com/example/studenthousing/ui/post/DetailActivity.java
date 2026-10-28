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
import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.local.UniversityData;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.University;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.databinding.ActivityDetailBinding;
import com.example.studenthousing.ui.adapter.PhotoPagerAdapter;
import com.example.studenthousing.ui.chat.ChatActivity;
import com.example.studenthousing.ui.exchange.ProposalActivity;
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

    private TextView tvTitle, tvLocation, tvHostName, tvDescription,
            tvPhotoCounter, tvDescriptionLabel,
            tvStatusBadge, tvHostRating, tvAreaMeta;
    private View tvHostVerified;
    private View cardHost, cardDistance, cardDescription, detailBottomBar;
    private View btnChat, btnProposeSwap;
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
        tvDescription = findViewById(R.id.tvDescription);
        tvDescriptionLabel = null;
        tvPhotoCounter = findViewById(R.id.tvPhotoCounter);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvAreaMeta = findViewById(R.id.tvAreaMeta);
        tvHostVerified = findViewById(R.id.tvHostVerified);
        tvHostRating = findViewById(R.id.tvHostRating);
        cardHost = findViewById(R.id.cardHost);
        cardDistance = findViewById(R.id.cardDistance);
        cardDescription = null;
        detailBottomBar = null;
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
        } else {
            String houseTitle = getIntent().getStringExtra("house_title");
            String houseAddress = getIntent().getStringExtra("house_address");
            if (houseTitle != null && tvTitle != null) tvTitle.setText(houseTitle);
            if (houseAddress != null && tvLocation != null) tvLocation.setText(houseAddress);
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

        btnProposeSwap = findViewById(R.id.btnProposeSwap);
        if (btnProposeSwap != null) btnProposeSwap.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ProposalActivity.class);
            if (currentPost != null) {
                intent.putExtra("receiver_post_id", currentPost.getPostId());
                intent.putExtra("receiver_user_id", currentPost.getUserId());
                intent.putExtra("HOST_NAME", currentPost.getFamilyName());
            } else {
                intent.putExtra("HOST_NAME", getString(R.string.detail_host_name));
            }
            startActivity(intent);
        });
    }

    private void renderPostImmediately(AccommodationPost post) {
        if (post == null) return;
        currentPost = post;
        if (tvTitle != null && post.getTitle() != null) tvTitle.setText(post.getTitle());
        if (tvLocation != null) tvLocation.setText(formatPublicAddress(post, false));
        if (tvHostName != null && post.getFamilyName() != null) tvHostName.setText(post.getFamilyName());
        bindPostMetadata(post);
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

    private void bindPostMetadata(AccommodationPost post) {
        if (tvAreaMeta != null) {
            boolean addressVerified = PostDisplayFormatter.isAddressVerified(post);
            if (addressVerified) tvAreaMeta.setText("Đã xác thực địa chỉ");
            tvAreaMeta.setVisibility(addressVerified ? View.VISIBLE : View.GONE);
        }
        if (tvStatusBadge != null) {
            boolean available = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());
            tvStatusBadge.setText(available ? "Sẵn sàng đổi" : "Đã đổi");
            tvStatusBadge.setBackgroundResource(available
                    ? R.drawable.bg_badge_verified
                    : R.drawable.bg_badge_distance);
            tvStatusBadge.setTextColor(getColor(available
                    ? R.color.olive_primary_dark
                    : R.color.text_sub));
            tvStatusBadge.setVisibility(View.VISIBLE);
        }
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
        boolean available = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());
        boolean canContact = !isOwner && available;
        if (btnChat != null) btnChat.setVisibility(canContact ? View.VISIBLE : View.GONE);
        if (btnProposeSwap != null) btnProposeSwap.setVisibility(canContact ? View.VISIBLE : View.GONE);
        if (detailBottomBar != null) detailBottomBar.setVisibility(canContact ? View.VISIBLE : View.GONE);
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
        boolean hasActiveExchange = isOwner;
        if (!isOwner && currentUid != null && post.getPostId() != null) {
            ExchangeDao exchangeDao = new ExchangeDao(this);
            hasActiveExchange = hasActiveExchangeForPost(exchangeDao.getBySender(currentUid), post.getPostId())
                    || hasActiveExchangeForPost(exchangeDao.getByReceiver(currentUid), post.getPostId());
        }

        if (tvLocation != null) {
            tvLocation.setText(formatPublicAddress(post, hasActiveExchange));
        }
        bindPostMetadata(post);
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
            }
            if (tvHostVerified != null) {
                tvHostVerified.setVisibility(user.isResidenceVerified() ? View.VISIBLE : View.GONE);
            }
        }

        List<Review> reviews = data.hostReviews;
        if (reviews == null || reviews.isEmpty()) {
            if (tvHostRating != null) tvHostRating.setText("Chưa có đánh giá");
            if (ivStar != null) ivStar.setVisibility(View.GONE);
        } else {
            double sum = 0;
            for (Review r : reviews) sum += r.getRating();
            double avg = sum / reviews.size();
            if (tvHostRating != null) {
                tvHostRating.setText(String.format(java.util.Locale.US, "%.1f (%d đánh giá)", avg, reviews.size()));
            }
            if (ivStar != null) ivStar.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasActiveExchangeForPost(List<ExchangeRequest> requests, String postId) {
        if (requests == null) return false;
        for (ExchangeRequest request : requests) {
            if (request == null || !(Constants.REQ_EXCHANGING.equalsIgnoreCase(request.getStatus()))) continue;
            if (postId.equals(request.getSenderPostId()) || postId.equals(request.getReceiverPostId())) return true;
        }
        return false;
    }

    private void setupNearbyCampuses(List<NearbyCampus> nearby) {
        LinearLayout container = null;
        if (container == null) return;
        container.removeAllViews();
        if (nearby != null) {
            for (NearbyCampus campus : nearby) {
                if (campus == null || campus.getCampusId() == null || campus.getKm() <= 0) continue;
                University university = UniversityData.findByCode(campus.getCampusId());
                String name = university == null ? campus.getCampusId() : university.getDisplayName();
                String distance = String.format(java.util.Locale.getDefault(), "%.1f km", campus.getKm());
                TextView item = new TextView(this);
                item.setText("Cách " + name + ": " + (campus.isEstimate() ? "~" + distance : distance));
                item.setTextColor(getColor(R.color.surface_card));
                item.setTextSize(13);
                item.setPadding(48, 4, 0, 4);
                container.addView(item);
            }
        }
        if (cardDistance != null) {
            cardDistance.setVisibility(container.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupAmenities(List<String> utilities) {
        if (gridAmenities == null) return;
        gridAmenities.removeAllViews();
        if (utilities == null || utilities.isEmpty()) {
            if (tvAmenitiesEmpty != null) tvAmenitiesEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvAmenitiesEmpty != null) tvAmenitiesEmpty.setVisibility(View.GONE);

        for (String util : utilities) {
            View item = getLayoutInflater().inflate(R.layout.item_amenity, gridAmenities, false);
            ImageView icon = item.findViewById(R.id.ivAmenityIcon);
            TextView label = item.findViewById(R.id.tvAmenityLabel);

            label.setText(util);
            icon.setImageResource(getIconForUtility(util));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            item.setLayoutParams(params);

            gridAmenities.addView(item);
        }
    }

    private int getIconForUtility(String u) {
        if (u == null) return R.drawable.ic_star;
        String key = u.toLowerCase();
        if (key.contains("wifi")) return R.drawable.ic_wifi;
        if (key.contains("điều hòa") || key.contains("máy lạnh")) return R.drawable.ic_air_conditioner;
        if (key.contains("bếp")) return R.drawable.ic_kitchen;
        if (key.contains("đỗ xe") || key.contains("gửi xe")) return R.drawable.ic_parking_car;
        if (key.contains("nóng lạnh") || key.contains("máy giặt")) return R.drawable.ic_verified;
        if (key.contains("ban công")) return R.drawable.ic_location;
        if (key.contains("bảo vệ") || key.contains("an ninh") || key.contains("camera")) return R.drawable.ic_shield_check;
        if (key.contains("thang máy")) return R.drawable.ic_chevron_right;
        if (key.contains("tủ lạnh")) return R.drawable.ic_kitchen;
        if (key.contains("gác") || key.contains("wc") || key.contains("giờ giấc")) return R.drawable.ic_check;
        return R.drawable.ic_check;
    }

    // ==================== PHOTO PAGER + DOT INDICATOR ====================

    private void setupPhotoPager(List<String> imageUrls) {
        List<String> safeUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        binding.photoPager.setAdapter(new PhotoPagerAdapter(safeUrls, position -> {
            showFullScreenPhoto(safeUrls, position);
        }));

        // ✅ Không dùng TabLayoutMediator nữa — tạo dot thủ công
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

    /** Tạo N chấm tròn 8dp, cách nhau 4dp, chấm đầu được chọn */
    private void setupDotIndicator(int totalImages) {
        LinearLayout dots = findViewById(R.id.photoIndicator);
        if (dots == null) return;
        dots.removeAllViews();
        if (totalImages <= 0) return;

        int size = dp(8);
        int gap  = dp(4);

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

    /** Đổi trạng thái selected cho đúng chấm đang xem */
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
        if (urls == null || urls.isEmpty()) return;
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_photo);

        ViewPager2 pager = dialog.findViewById(R.id.fullScreenPager);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullScreen);
        TextView tvCounter = dialog.findViewById(R.id.tvFullScreenCounter);

        pager.setAdapter(new PhotoPagerAdapter(urls));
        pager.setCurrentItem(startPos, false);

        if (tvCounter != null) {
            tvCounter.setText((startPos + 1) + " / " + urls.size());
            pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    tvCounter.setText((position + 1) + " / " + urls.size());
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void toggleFavorite() {
        viewModel.toggleFavorite(postId);
    }
}