package com.example.studenthousing.ui.review;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.ui.adapter.SelectedPhotoAdapter;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.CloudinaryUploader;
import com.example.studenthousing.util.FileUtils;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    public static final String EXTRA_TARGET_USER_NAME = "target_user_name";

    private String targetUserId;
    private String targetUserName;

    private ReviewRepository reviewRepository;
    private SessionManager session;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private SelectedPhotoAdapter photoAdapter;
    private RecyclerView rvReviewPhotos;
    private ActivityResultLauncher<String> multiImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        reviewRepository = new ReviewRepository(this);
        session = new SessionManager(this);

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        targetUserName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextView tvRatingText = findViewById(R.id.tvRatingText);
        EditText edtComment = findViewById(R.id.edtComment);
        com.google.android.material.button.MaterialButton btnSubmitReview = findViewById(R.id.btnSubmitReview);
        View btnAddReviewPhoto = null;
        rvReviewPhotos = null;

        if (targetUserName != null && !targetUserName.isEmpty()) {
            if (tvPartnerName != null) tvPartnerName.setText(targetUserName);
        }

        if (rvReviewPhotos != null) {
            rvReviewPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            photoAdapter = new SelectedPhotoAdapter(selectedImageUris, position -> {
                if (position >= 0 && position < selectedImageUris.size()) {
                    selectedImageUris.remove(position);
                    updatePhotoPreview();
                }
            });
            rvReviewPhotos.setAdapter(photoAdapter);
        }

        multiImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        for (Uri uri : uris) {
                            if (uri != null && !selectedImageUris.contains(uri)) {
                                selectedImageUris.add(uri);
                            }
                        }
                        updatePhotoPreview();
                    }
                }
        );

        if (btnAddReviewPhoto != null) {
            btnAddReviewPhoto.setOnClickListener(v -> {
                if (multiImagePickerLauncher != null) {
                    multiImagePickerLauncher.launch("image/*");
                }
            });
        }

        String[] ratingLabels = {"", "Rất không hài lòng", "Không hài lòng", "Bình thường", "Hài lòng", "Rất hài lòng"};

        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                int intRating = Math.max(1, Math.round(rating));
                if (tvRatingText != null) tvRatingText.setText(ratingLabels[intRating]);
            });
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> {
                int rating = ratingBar != null ? Math.max(1, Math.round(ratingBar.getRating())) : 5;
                String comment = edtComment != null ? edtComment.getText().toString().trim() : "";
                if (comment.isEmpty()) {
                    comment = "Đánh giá " + rating + " sao cho đối tác.";
                }

                btnSubmitReview.setEnabled(false);
                btnSubmitReview.setText("Đang tải ảnh và gửi...");

                if (selectedImageUris.isEmpty()) {
                    submitReviewWithImages(rating, comment, new ArrayList<>(), btnSubmitReview);
                } else {
                    uploadReviewImages(0, new ArrayList<>(), rating, comment, btnSubmitReview);
                }
            });
        }
    }

    private void updatePhotoPreview() {
        if (selectedImageUris.isEmpty()) {
            if (rvReviewPhotos != null) rvReviewPhotos.setVisibility(View.GONE);
        } else {
            if (rvReviewPhotos != null) rvReviewPhotos.setVisibility(View.VISIBLE);
        }
        if (photoAdapter != null) {
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void uploadReviewImages(int index, List<String> uploadedUrls, int rating, String comment, MaterialButton btnSubmit) {
        if (index >= selectedImageUris.size()) {
            submitReviewWithImages(rating, comment, uploadedUrls, btnSubmit);
            return;
        }

        File imageFile = FileUtils.getFileFromUri(this, selectedImageUris.get(index));
        if (imageFile == null) {
            handleImageUploadFailure(btnSubmit, new IllegalStateException("Không thể đọc ảnh đã chọn"));
            return;
        }

        CloudinaryUploader.uploadImage(imageFile, new AppCallback<String>() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedUrls.add(imageUrl);
                uploadReviewImages(index + 1, uploadedUrls, rating, comment, btnSubmit);
            }

            @Override
            public void onError(Exception e) {
                handleImageUploadFailure(btnSubmit, e);
            }
        });
    }

    private void handleImageUploadFailure(MaterialButton btnSubmit, Exception error) {
        String message = error != null && error.getMessage() != null
                ? error.getMessage() : "Vui lòng thử lại";
        runOnUiThread(() -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi đánh giá");
            Toast.makeText(this, "Tải ảnh đánh giá thất bại: " + message, Toast.LENGTH_LONG).show();
        });
    }

    private void submitReviewWithImages(int rating, String comment, List<String> imageUrls, MaterialButton btnSubmit) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi đánh giá");
            Toast.makeText(this, "Vui lòng đăng nhập lại để gửi đánh giá", Toast.LENGTH_LONG).show();
            return;
        }
        String currentUserId = firebaseUser.getUid();
        String currentUserName = session.getUserFamilyName() != null ? session.getUserFamilyName() : "Người dùng";

        if (targetUserId == null || targetUserId.trim().isEmpty() || currentUserId.equals(targetUserId)) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi đánh giá");
            Toast.makeText(this, "Vui lòng chọn người nhận đánh giá hợp lệ", Toast.LENGTH_LONG).show();
            return;
        }

        Review review = new Review();
        review.setReviewId(targetUserId + "_" + currentUserId);
        review.setRevieweeId(targetUserId != null ? targetUserId : "target_user");
        review.setReviewerId(currentUserId);
        review.setReviewerName(currentUserName);
        review.setRating(rating);
        review.setComment(comment);
        review.setImageUrls(imageUrls);
        long now = System.currentTimeMillis();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        reviewRepository.create(review, new AppCallback<String>() {
            @Override
            public void onSuccess(String reviewId) {
                runOnUiThread(() -> {
                    Toast.makeText(ReviewActivity.this, "Gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Gửi đánh giá");
                    Toast.makeText(ReviewActivity.this, "Gửi đánh giá thất bại: " +
                            (e.getMessage() != null ? e.getMessage() : "Vui lòng thử lại"), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
