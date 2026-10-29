package com.example.studenthousing.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.databinding.ActivityMyPostsBinding;
import com.example.studenthousing.ui.adapter.MyPostAdapter;
import com.example.studenthousing.ui.viewmodel.MyPostsViewModel;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity implements MyPostAdapter.OnPostActionListener {

    private ActivityMyPostsBinding binding;
    private SessionManager session;
    private MyPostsViewModel viewModel;
    private MyPostAdapter postAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(MyPostsViewModel.class);
        viewModel.getState().observe(this, this::renderPosts);

        binding.btnBack.setOnClickListener(v -> finish());

        if (binding.rvMyPosts != null) {
            binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
            postAdapter = new MyPostAdapter(this);
            binding.rvMyPosts.setAdapter(postAdapter);
        }

        View btnCreatePost = findViewById(R.id.btnCreatePost);
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v ->
                    startActivity(new Intent(MyPostsActivity.this, CreatePostActivity.class)));
        }

        loadMyPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyPosts();
    }

    private void loadMyPosts() {
        String currentUserId = session == null ? null : session.getUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại để xem bài đăng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel.load(currentUserId);
    }

    private void renderPosts(UiState<List<AccommodationPost>> state) {
        if (state != null && state.getContent() != null) displayPosts(state.getContent());
    }

    private void displayPosts(List<AccommodationPost> posts) {
        if (postAdapter == null) return;
        if (posts != null && !posts.isEmpty()) {
            binding.tvTotalPosts.setText(posts.size() + " bài đăng");
            postAdapter.submitList(posts);
            binding.rvMyPosts.setVisibility(View.VISIBLE);
            if (binding.layoutMyPostsEmpty != null) binding.layoutMyPostsEmpty.setVisibility(View.GONE);
        } else {
            binding.tvTotalPosts.setText("0 bài đăng");
            postAdapter.submitList(new ArrayList<>());
            binding.rvMyPosts.setVisibility(View.GONE);
            if (binding.layoutMyPostsEmpty != null) binding.layoutMyPostsEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewPost(@NonNull AccommodationPost post) {
        Intent intent = new Intent(MyPostsActivity.this, DetailActivity.class);
        intent.putExtra("post_id", post.getPostId());
        intent.putExtra("POST_ID", post.getPostId());
        intent.putExtra("house_title", post.getTitle());
        intent.putExtra("house_address", post.getAddress());
        startActivity(intent);
    }

    @Override
    public void onEditPost(@NonNull AccommodationPost post) {
        Intent intent = new Intent(MyPostsActivity.this, CreatePostActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        intent.putExtra("POST_ID", post.getPostId());
        intent.putExtra("POST_TITLE", post.getTitle());
        intent.putExtra("POST_ADDRESS", post.getAddress());
        String ward = post.getWardName() == null ? "" : post.getWardName().trim();
        String city = post.getCityName() == null ? "" : post.getCityName().trim();
        String administrativeAddress = ward.isEmpty() ? city
                : city.isEmpty() ? ward : ward + ", " + city;
        intent.putExtra("POST_ADMIN_ADDRESS", administrativeAddress);
        intent.putExtra("POST_CITY_CODE", post.getCityCode());
        intent.putExtra("POST_WARD_CODE", post.getWardCode());
        intent.putExtra("POST_WARD_NAME", post.getWardName());
        intent.putExtra("POST_HOUSE_NUMBER", post.getHouseNumber());
        intent.putExtra("POST_STREET_NAME", post.getStreetName());
        intent.putExtra("POST_STREET_ADDRESS",
                administrativeAddress.isEmpty() ? "" : getStreetAddress(post));
        intent.putStringArrayListExtra("POST_UTILITIES",
                post.getUtilities() == null ? new ArrayList<>() : new ArrayList<>(post.getUtilities()));
        intent.putStringArrayListExtra("POST_IMAGES",
                post.getImages() == null ? new ArrayList<>() : new ArrayList<>(post.getImages()));
        intent.putExtra("POST_DESC", post.getDescription());
        startActivity(intent);
    }

    private String getStreetAddress(AccommodationPost post) {
        String address = post.getAddress() == null ? "" : post.getAddress().trim();
        String ward = post.getWardName() == null ? "" : post.getWardName().trim();
        String city = post.getCityName() == null ? "" : post.getCityName().trim();
        if (!ward.isEmpty()) {
            int wardSeparator = address.lastIndexOf(", " + ward + ", ");
            if (wardSeparator >= 0) return address.substring(0, wardSeparator);
        } else if (!city.isEmpty()) {
            String suffix = ", " + city;
            if (address.endsWith(suffix)) return address.substring(0, address.length() - suffix.length());
        }
        return address;
    }

    @Override
    public void onDeletePost(@NonNull AccommodationPost post) {
        String title = post.getTitle() != null ? post.getTitle() : "bài đăng";
        new AlertDialog.Builder(MyPostsActivity.this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa bài đăng '" + title + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.delete(post.getPostId(), new AppCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                Toast.makeText(MyPostsActivity.this, "Đã xóa bài đăng thành công!", Toast.LENGTH_SHORT).show();
                                loadMyPosts();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(MyPostsActivity.this,
                                        "Lỗi xoá bài đăng", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
