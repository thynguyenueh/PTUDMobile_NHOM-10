package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {

    private SessionManager session;
    private PostRepository postRepository;
    private LinearLayout layoutPostsContainer;
    private TextView tvTotalPosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        session = new SessionManager(this);
        postRepository = new PostRepository(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutPostsContainer = findViewById(R.id.layoutPostsContainer);
        tvTotalPosts = findViewById(R.id.tvTotalPosts);

        View btnCreatePost = findViewById(R.id.btnCreatePost);
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v ->
                    startActivity(new Intent(MyPostsActivity.this, RegisterStep2Activity.class)));
        }

        loadMyPosts();
    }

    private void loadMyPosts() {
        String currentUserId = session != null ? session.getUserId() : null;
        postRepository.getLatestPosts(50, new AppCallback<List<AccommodationPost>>() {
            @Override
            public void onSuccess(List<AccommodationPost> posts) {
                List<AccommodationPost> myPosts = new ArrayList<>();
                if (posts != null) {
                    for (AccommodationPost p : posts) {
                        if (currentUserId == null || (p.getUserId() != null && p.getUserId().equals(currentUserId)) || p.getPostId().startsWith("post_")) {
                            myPosts.add(p);
                        }
                    }
                }
                runOnUiThread(() -> displayPosts(myPosts));
            }

            @Override
            public void onError(Exception e) {
                List<AccommodationPost> cached = postRepository.getCachedPosts();
                runOnUiThread(() -> displayPosts(cached));
            }
        });
    }

    private void displayPosts(List<AccommodationPost> posts) {
        if (layoutPostsContainer == null) return;
        layoutPostsContainer.removeAllViews();

        if (posts != null && !posts.isEmpty()) {
            tvTotalPosts.setText(posts.size() + " bài đăng của bạn");
            for (AccommodationPost p : posts) {
                addPostItemView(
                        p.getTitle() != null ? p.getTitle() : "Bài đăng nhà ở",
                        "Giá: " + p.getPrice() + " Tr/tháng · " + p.getArea() + " m²",
                        p.getAddress() != null ? p.getAddress() : "Chưa cập nhật địa chỉ",
                        "● " + (p.getStatus() != null ? p.getStatus() : "Đang hiển thị")
                );
            }
        } else {
            // Mẫu nếu chưa có dữ liệu DB/Firestore
            tvTotalPosts.setText("2 bài đăng đang hiển thị");

            addPostItemView(
                    "Nhà trọ sinh viên gần ĐHQG - Linh Trung",
                    "Giá: 2.5 Tr/tháng · 35 m²",
                    "Địa chỉ: Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh",
                    "● Đang hiển thị"
            );

            addPostItemView(
                    "Căn hộ gia đình 2PN cao cấp - Thảo Điền",
                    "Giá: 5.0 Tr/tháng · 65 m²",
                    "Địa chỉ: Phường Thảo Điền, Quận 2, TP. Hồ Chí Minh",
                    "● Đang hiển thị"
            );
        }
    }

    private void addPostItemView(String title, String priceArea, String address, String status) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_my_post, layoutPostsContainer, false);

        TextView tvTitle = itemView.findViewById(R.id.tvPostTitle);
        TextView tvPriceArea = itemView.findViewById(R.id.tvPostPriceArea);
        TextView tvAddress = itemView.findViewById(R.id.tvPostAddress);
        TextView tvStatus = itemView.findViewById(R.id.tvPostStatus);

        View btnViewPost = itemView.findViewById(R.id.btnViewPost);
        View btnEditPost = itemView.findViewById(R.id.btnEditPost);

        tvTitle.setText(title);
        tvPriceArea.setText(priceArea);
        tvAddress.setText(address);
        tvStatus.setText(status);

        if (btnViewPost != null) {
            btnViewPost.setOnClickListener(v ->
                    startActivity(new Intent(MyPostsActivity.this, DetailActivity.class)));
        }

        if (btnEditPost != null) {
            btnEditPost.setOnClickListener(v -> {
                Intent intent = new Intent(MyPostsActivity.this, RegisterStep2Activity.class);
                intent.putExtra("IS_EDIT_MODE", true);
                intent.putExtra("POST_TITLE", title);
                startActivity(intent);
            });
        }

        layoutPostsContainer.addView(itemView);
    }
}
