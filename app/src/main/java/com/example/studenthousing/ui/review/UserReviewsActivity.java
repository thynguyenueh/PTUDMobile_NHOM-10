package com.example.studenthousing.ui.review;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.databinding.ActivityUserReviewsBinding;
import com.example.studenthousing.ui.adapter.UserReviewAdapter;
import com.example.studenthousing.ui.viewmodel.UserReviewsViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;

public class UserReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    public static final String EXTRA_TARGET_USER_NAME = "target_user_name";

    private ActivityUserReviewsBinding binding;
    private SessionManager session;
    private UserReviewsViewModel viewModel;
    private UserReviewAdapter reviewAdapter;

    private String targetUserId;
    private String targetUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(UserReviewsViewModel.class);

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        targetUserName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);

        binding.btnBack.setOnClickListener(v -> finish());

        if (binding.rvUserReviews != null) {
            binding.rvUserReviews.setLayoutManager(new LinearLayoutManager(this));
            reviewAdapter = new UserReviewAdapter();
            binding.rvUserReviews.setAdapter(reviewAdapter);
        }

        TextView reviewsState = null;
        viewModel.getReviews().observe(this, state -> {
            if (state == null) return;
            if (state.getScreenState() == UiState.ScreenState.LOADING) {
                if (reviewsState != null) {
                    reviewsState.setText("Đang tải đánh giá...");
                    reviewsState.setVisibility(View.VISIBLE);
                }
            } else if (state.getScreenState() == UiState.ScreenState.ERROR) {
                if (reviewsState != null) {
                    reviewsState.setText(state.getErrorMessage() != null
                            ? state.getErrorMessage() : "Không thể tải đánh giá");
                    reviewsState.setVisibility(View.VISIBLE);
                }
            } else {
                ArrayList<Review> reviews = state.getContent() == null
                        ? new ArrayList<>() : new ArrayList<>(state.getContent());
                if (reviewAdapter != null) reviewAdapter.submitList(reviews);
                if (reviewsState != null) {
                    if (reviews.isEmpty()) {
                        reviewsState.setText("Chưa có đánh giá nào");
                        reviewsState.setVisibility(View.VISIBLE);
                    } else {
                        reviewsState.setVisibility(View.GONE);
                    }
                }
            }
        });

        String currentUserId = session.getUserId();
        if (targetUserId == null || targetUserId.trim().isEmpty()) targetUserId = currentUserId;
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            finish();
            return;
        }
        boolean isOtherUser = currentUserId != null && !targetUserId.equals(currentUserId);

        if (binding.layoutBottomAction != null) {
            binding.layoutBottomAction.setVisibility(isOtherUser ? View.VISIBLE : View.GONE);
        }

        if (binding.btnWriteReview != null) {
            binding.btnWriteReview.setOnClickListener(v -> {
                if (targetUserId == null || targetUserId.trim().isEmpty() || targetUserId.equals(currentUserId)) return;
                Intent intent = new Intent(UserReviewsActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_ID, "req_general");
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, targetUserId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, targetUserName != null ? targetUserName : "Đối tác đổi nhà");
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
    }

    private void loadReviews() {
        String userIdToFetch = targetUserId;
        if (userIdToFetch == null || userIdToFetch.trim().isEmpty() || viewModel == null) return;
        viewModel.load(userIdToFetch, true);
    }
}
