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
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.databinding.ActivityDetailBinding;
import com.example.studenthousing.ui.adapter.PhotoPagerAdapter;
import com.example.studenthousing.ui.chat.ChatActivity;
import com.example.studenthousing.ui.exchange.ProposalActivity;
import com.example.studenthousing.ui.review.UserReviewsActivity;
import com.example.studenthousing.ui.viewmodel.DetailViewModel;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
            tvStatusBadge, tvAreaMeta;
    private View tvHostVerified;
    private View cardHost, cardDistance, cardDescription, detailBottomBar;
    private View btnChat;
    private GridLayout gridAmenities;
    private TextView tvAmenitiesEmpty;

    private EditText etCommentInput;
    private View btnSubmitComment;
    private RecyclerView rvComments;
    private TextView tvCommentsEmpty;
    private com.example.studenthousing.ui.adapter.UserReviewAdapter commentAdapter;
    private com.example.studenthousing.data.repository.ReviewRepository reviewRepository;

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
        cardHost = findViewById(R.id.cardHost);
        cardDistance = findViewById(R.id.cardDistance);
        cardDescription = null;
        detailBottomBar = null;
        gridAmenities = findViewById(R.id.gridAmenities);
        tvAmenitiesEmpty = findViewById(R.id.tvAmenitiesEmpty);

        etCommentInput = findViewById(R.id.etCommentInput);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        rvComments = findViewById(R.id.rvComments);
        tvCommentsEmpty = findViewById(R.id.tvCommentsEmpty);

        reviewRepository = new com.example.studenthousing.data.repository.ReviewRepository(this);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            commentAdapter = new com.example.studenthousing.ui.adapter.UserReviewAdapter();
            rvComments.setAdapter(commentAdapter);
        }

        if (btnSubmitComment != null) {
            btnSubmitComment.setOnClickListener(v -> submitComment());
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnFavorite = findViewById(R.id.btnFavorite);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> toggleFavorite());

        if (cardHost != null) {
            cardHost.setOnClickListener(v -> {
                if (currentPost == null || currentPost.getUserId() == null || currentPost.getUserId().trim().isEmpty()) return;
                Intent intent = new Intent(DetailActivity.this, com.example.studenthousing.ui.partner.PublicHostProfileActivity.class);
                intent.putExtra(com.example.studenthousing.ui.partner.PublicHostProfileActivity.EXTRA_TARGET_USER_ID, currentPost.getUserId());
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

        View btnCall = findViewById(R.id.btnCall);
        if (btnCall != null) btnCall.setOnClickListener(v -> {
            if (currentPost != null && currentPost.getUserId() != null) {
                com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(DetailActivity.this);
                User host = userDao.getById(currentPost.getUserId());
                String phone = host != null && host.getPhone() != null && !host.getPhone().isEmpty()
                        ? host.getPhone() : "0988 123 456";
                String hostName = currentPost.getFamilyName() != null ? currentPost.getFamilyName() : "Chủ bài đăng";
                new androidx.appcompat.app.AlertDialog.Builder(DetailActivity.this)
                        .setTitle("Gọi điện cho người đăng bài")
                        .setMessage("Chủ nhà: " + hostName + "\nSố điện thoại: " + phone)
                        .setPositiveButton("Gọi ngay", (dialog, which) -> {
                            Intent callIntent = new Intent(Intent.ACTION_DIAL, android.net.Uri.fromParts("tel", phone.trim(), null));
                            startActivity(callIntent);
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
            }
        });

        btnChat = findViewById(R.id.btnChat);
        if (btnChat != null) btnChat.setOnClickListener(v -> {
            if (currentPost != null) {
                openSmsApp(currentPost.getUserId(), "Xin chào, tôi quan tâm đến bài đăng của bạn.");
            }
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
        loadComments();
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
        if (detailBottomBar != null) detailBottomBar.setVisibility(View.VISIBLE);

        View btnCall = findViewById(R.id.btnCall);
        View btnChat = findViewById(R.id.btnChat);

        if (isOwner) {
            if (btnCall instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnCall).setText("Chỉnh sửa");
                btnCall.setVisibility(View.VISIBLE);
                btnCall.setOnClickListener(v -> {
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
                    intent.putStringArrayListExtra("POST_UTILITIES", post.getUtilities() != null ? new ArrayList<>(post.getUtilities()) : new ArrayList<>());
                    startActivity(intent);
                });
            }
            if (btnChat instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnChat).setText("Xóa bài");
                btnChat.setVisibility(View.VISIBLE);
                btnChat.setOnClickListener(v -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Xóa bài đăng?")
                            .setMessage("Bạn có chắc chắn muốn xóa bài đăng này không?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                new PostRepository(this).deletePost(post.getPostId(), new AppCallback<Void>() {
                                    @Override public void onSuccess(Void d) {
                                        runOnUiThread(() -> {
                                            android.widget.Toast.makeText(DetailActivity.this, "Đã xóa bài đăng", android.widget.Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                                    }
                                    @Override public void onError(Exception e) {
                                        runOnUiThread(() -> {
                                            android.widget.Toast.makeText(DetailActivity.this, "Đã xóa bài đăng", android.widget.Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        } else {
            boolean available = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());
            if (btnCall instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnCall).setText(R.string.action_call);
                btnCall.setVisibility(available ? View.VISIBLE : View.GONE);
                btnCall.setOnClickListener(v -> {
                    if (currentPost != null && currentPost.getUserId() != null) {
                        com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(DetailActivity.this);
                        User host = userDao.getById(currentPost.getUserId());
                        String phone = host != null && host.getPhone() != null && !host.getPhone().isEmpty()
                                ? host.getPhone() : "0988 123 456";
                        String hostName = currentPost.getFamilyName() != null ? currentPost.getFamilyName() : "Chủ bài đăng";
                        new androidx.appcompat.app.AlertDialog.Builder(DetailActivity.this)
                                .setTitle("Gọi điện cho người đăng bài")
                                .setMessage("Chủ nhà: " + hostName + "\nSố điện thoại: " + phone)
                                .setPositiveButton("Gọi ngay", (dialog, which) -> {
                                    Intent callIntent = new Intent(Intent.ACTION_DIAL, android.net.Uri.fromParts("tel", phone.trim(), null));
                                    startActivity(callIntent);
                                })
                                .setNegativeButton("Đóng", null)
                                .show();
                    }
                });
            }
            if (btnChat instanceof com.google.android.material.button.MaterialButton) {
                ((com.google.android.material.button.MaterialButton) btnChat).setText(R.string.action_message);
                btnChat.setVisibility(available ? View.VISIBLE : View.GONE);
                btnChat.setOnClickListener(v -> {
                    if (currentPost != null) {
                        openSmsApp(currentPost.getUserId(), "Xin chào, tôi quan tâm đến bài đăng của bạn.");
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

        loadComments();
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

    private void loadComments() {
        if (currentPost == null) return;
        String key = currentPost.getPostId() != null ? currentPost.getPostId() : currentPost.getUserId();
        if (key == null) return;

        com.example.studenthousing.data.local.ReviewDao dao = new com.example.studenthousing.data.local.ReviewDao(DetailActivity.this);
        List<Review> local = dao.getByReviewee(key);
        if (local == null || local.isEmpty()) {
            local = dao.getByRequest(key);
        }
        if ((local == null || local.isEmpty()) && currentPost.getUserId() != null) {
            local = dao.getByReviewee(currentPost.getUserId());
        }

        final List<Review> displayList = local != null ? local : new ArrayList<>();
        if (commentAdapter != null) commentAdapter.submitList(displayList);
        if (rvComments != null) rvComments.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
        if (tvCommentsEmpty != null) tvCommentsEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);

        reviewRepository.getByReviewee(key, new AppCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    List<Review> freshLocal = dao.getByReviewee(key);
                    if (freshLocal != null && !freshLocal.isEmpty()) {
                        if (commentAdapter != null) commentAdapter.submitList(freshLocal);
                        if (rvComments != null) rvComments.setVisibility(View.VISIBLE);
                        if (tvCommentsEmpty != null) tvCommentsEmpty.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void submitComment() {
        if (currentPost == null || currentPost.getPostId() == null) {
            Toast.makeText(this, "Chưa tải xong thông tin bài đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = SessionManager.getUid(this);
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }
        String content = etCommentInput != null ? etCommentInput.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(this);
        User reviewer = userDao.getById(currentUserId);
        String reviewerName = reviewer != null && reviewer.getFamilyName() != null ? reviewer.getFamilyName() : "Người dùng";

        String targetKey = currentPost.getPostId();

        Review r = new Review();
        r.setRequestId(targetKey);
        r.setReviewerId(currentUserId);
        r.setReviewerName(reviewerName);
        r.setRevieweeId(targetKey);
        r.setRevieweeName(currentPost.getFamilyName() != null ? currentPost.getFamilyName() : "Chủ nhà");
        r.setRating(5.0);
        r.setComment(content);
        r.setCreatedAt(System.currentTimeMillis());

        // Save locally first so UI updates immediately
        com.example.studenthousing.data.local.ReviewDao dao = new com.example.studenthousing.data.local.ReviewDao(this);
        dao.upsert(r);

        if (etCommentInput != null) etCommentInput.setText("");
        Toast.makeText(DetailActivity.this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
        loadComments();

        reviewRepository.create(r, new AppCallback<String>() {
            @Override public void onSuccess(String id) {}
            @Override public void onError(Exception e) {}
        });
    }

    private void openSmsApp(String targetUserId, String defaultMessage) {
        com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(this);
        User host = targetUserId != null ? userDao.getById(targetUserId) : null;
        String phone = host != null && host.getPhone() != null && !host.getPhone().isEmpty()
                ? host.getPhone() : "0988123456";
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + phone.trim()));
        smsIntent.putExtra("sms_body", defaultMessage);
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:" + phone.trim()));
                viewIntent.putExtra("sms_body", defaultMessage);
                startActivity(viewIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Không tìm thấy ứng dụng nhắn tin", Toast.LENGTH_SHORT).show();
            }
        }
    }
}