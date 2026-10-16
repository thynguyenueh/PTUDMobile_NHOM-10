package com.example.studenthousing;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    public static final String EXTRA_TARGET_USER_NAME = "target_user_name";
    public static final String EXTRA_TARGET_USER_ROLE = "target_user_role";
    public static final String EXTRA_REQUEST_INFO = "request_info";

    private TextView[] starViews;
    private TextView tvRatingLabel;
    private EditText edtReview;
    private TextView btnSubmit;
    private TextView tvUploadLabel;

    private TextView tvPartnerAvatar;
    private TextView tvPartnerRole;
    private TextView tvPartnerName;
    private TextView tvPartnerSubtext;

    private int currentRating = 5;
    private String selectedImageUri = null;
    private final Set<String> selectedTags = new HashSet<>();

    private ReviewRepository reviewRepository;
    private SessionManager session;
    private String requestId;
    private String targetUserId;

    private static final String[] RATING_TEXTS = {
            "",
            "Rất không hài lòng",
            "Không hài lòng",
            "Bình thường",
            "Hài lòng",
            "Rất hài lòng"
    };

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveImageToInternalStorage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);

        session = new SessionManager(this);
        reviewRepository = new ReviewRepository(this);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        setupPartnerInfo();

        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        setupStars();
        setupTags();

        edtReview = findViewById(R.id.edtReview);
        tvUploadLabel = findViewById(R.id.tvUploadLabel);

        if (findViewById(R.id.btnUploadImage) != null) {
            findViewById(R.id.btnUploadImage).setOnClickListener(v -> pickImage.launch("image/*"));
        }

        btnSubmit = findViewById(R.id.btnSubmit);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitReview());
        }

        // Khởi tạo rating mặc định 5 sao
        selectRating(5);
    }

    private void setupPartnerInfo() {
        tvPartnerAvatar = findViewById(R.id.tvPartnerAvatar);
        tvPartnerRole = findViewById(R.id.tvPartnerRole);
        tvPartnerName = findViewById(R.id.tvPartnerName);
        tvPartnerSubtext = findViewById(R.id.tvPartnerSubtext);

        String partnerName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);
        String partnerRole = getIntent().getStringExtra(EXTRA_TARGET_USER_ROLE);
        String requestInfo = getIntent().getStringExtra(EXTRA_REQUEST_INFO);

        if (partnerName != null && tvPartnerName != null) {
            tvPartnerName.setText(partnerName);
            if (tvPartnerAvatar != null && !partnerName.isEmpty()) {
                tvPartnerAvatar.setText(partnerName.substring(0, 1).toUpperCase());
            }
        }
        if (partnerRole != null && tvPartnerRole != null) {
            tvPartnerRole.setText(partnerRole.toUpperCase());
        }
        if (requestInfo != null && tvPartnerSubtext != null) {
            tvPartnerSubtext.setText(requestInfo);
        }
    }

    /** Cấu hình bấm trực tiếp từng ngôi sao (Click star N -> hiện N sao) */
    private void setupStars() {
        starViews = new TextView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        for (int i = 0; i < starViews.length; i++) {
            final int starRating = i + 1;
            if (starViews[i] != null) {
                starViews[i].setOnClickListener(v -> selectRating(starRating));
            }
        }
    }

    /** Chọn số sao N trực tiếp */
    private void selectRating(int rating) {
        currentRating = rating;
        for (int i = 0; i < starViews.length; i++) {
            if (starViews[i] != null) {
                starViews[i].setText(i < rating ? "★" : "☆");
                starViews[i].setTextColor(i < rating ? Color.parseColor("#FFC400") : Color.parseColor("#D1D5DB"));
            }
        }
        if (tvRatingLabel != null && rating >= 1 && rating <= 5) {
            tvRatingLabel.setText(RATING_TEXTS[rating]);
            tvRatingLabel.setTextColor(rating >= 4 ? Color.parseColor("#719B28") :
                    (rating == 3 ? Color.parseColor("#EAB308") : Color.parseColor("#EF4444")));
        }
    }

    /** Cấu hình chọn/bỏ chọn các điểm hài lòng (Tags) */
    private void setupTags() {
        // Tag 1 & 2 mặc định chọn sẵn
        selectedTags.add("Sạch sẽ");
        selectedTags.add("Thân thiện");

        bindTag(R.id.tagClean, "Sạch sẽ");
        bindTag(R.id.tagFriendly, "Thân thiện");
        bindTag(R.id.tagSupport, "Hỗ trợ tốt");
        bindTag(R.id.tagPunctual, "Đúng giờ");
        bindTag(R.id.tagSafe, "An ninh");

        TextView btnAddCustomTag = findViewById(R.id.btnAddCustomTag);
        if (btnAddCustomTag != null) {
            btnAddCustomTag.setOnClickListener(v -> showAddCustomTraitDialog());
        }
    }

    private void bindTag(int viewId, String tagText) {
        TextView tvTag = findViewById(viewId);
        if (tvTag == null) return;

        updateTagUi(tvTag, tagText, selectedTags.contains(tagText));

        tvTag.setOnClickListener(v -> {
            boolean selected = !selectedTags.contains(tagText);
            if (selected) {
                selectedTags.add(tagText);
            } else {
                selectedTags.remove(tagText);
            }
            updateTagUi(tvTag, tagText, selected);
        });
    }

    private void updateTagUi(TextView tvTag, String tagText, boolean isSelected) {
        tvTag.setText(isSelected ? "✓  " + tagText : tagText);
        if (isSelected) {
            tvTag.setBackgroundResource(R.drawable.bg_tag);
            tvTag.setTextColor(Color.parseColor("#719B28"));
        } else {
            tvTag.setBackgroundResource(R.drawable.bg_tag_unselected);
            tvTag.setTextColor(Color.parseColor("#6B7280"));
        }
    }

    private void showAddCustomTraitDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập điểm hài lòng (vd: Rộng rãi, Yên tĩnh...)");

        new AlertDialog.Builder(this)
                .setTitle("Thêm điểm hài lòng")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        selectedTags.add(text);
                        Toast.makeText(this, "Đã thêm đặc điểm: " + text, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /** Sao chép file ảnh đính kèm vào bộ nhớ trong của app để bảo lưu đường dẫn lâu dài */
    private void saveImageToInternalStorage(Uri uri) {
        new Thread(() -> {
            try {
                File dir = new File(getFilesDir(), "reviews");
                if (!dir.exists()) dir.mkdirs();
                File destFile = new File(dir, "review_" + System.currentTimeMillis() + ".jpg");

                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(destFile)) {
                    if (in != null) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                }

                selectedImageUri = destFile.getAbsolutePath();
                runOnUiThread(() -> {
                    if (tvUploadLabel != null) {
                        tvUploadLabel.setText("📷 Đã chọn 1 ảnh đính kèm (Lưu bộ nhớ)");
                    }
                    Toast.makeText(this, "Đã chọn và lưu ảnh đính kèm", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Không thể lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void submitReview() {
        String comment = edtReview != null && edtReview.getText() != null
                ? edtReview.getText().toString().trim() : "";

        // Ghép các tag hài lòng vào nội dung comment nếu có chọn
        if (!selectedTags.isEmpty()) {
            String tagInfo = "Điểm hài lòng: " + String.join(", ", selectedTags);
            if (comment.isEmpty()) {
                comment = tagInfo;
            } else {
                comment = comment + "\n(" + tagInfo + ")";
            }
        }

        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
        }

        Review r = new Review();
        r.setReviewId(UUID.randomUUID().toString());
        r.setRequestId(requestId);
        String currentUserId = session.getUserId() != null ? session.getUserId() : "current_user";
        r.setReviewerId(currentUserId);
        r.setRevieweeId(targetUserId != null ? targetUserId : "demo_target_user");
        r.setRating(currentRating);
        r.setComment(comment);
        r.setImageUri(selectedImageUri);
        r.setCreatedAt(System.currentTimeMillis());

        boolean isOnline = NetworkUtils.isOnline(this);

        reviewRepository.create(r, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                runOnUiThread(() -> {
                    Toast.makeText(ReviewActivity.this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (btnSubmit != null) btnSubmit.setEnabled(true);
                    if (!isOnline) {
                        // Offline mode: review was cached to SQLite database successfully
                        Toast.makeText(ReviewActivity.this, "Đã lưu bản nháp đánh giá (offline). Sẽ đồng bộ khi có mạng.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        // Real error occurred
                        String msg = e != null ? e.getMessage() : "Lỗi hệ thống";
                        Toast.makeText(ReviewActivity.this, "Không thể gửi đánh giá: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
