package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.util.Constants;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MyPostAdapter extends RecyclerView.Adapter<MyPostAdapter.MyPostViewHolder> {

    public interface OnPostActionListener {
        void onViewPost(@NonNull AccommodationPost post);
        void onEditPost(@NonNull AccommodationPost post);
        void onDeletePost(@NonNull AccommodationPost post);
        void onToggleStatus(@NonNull AccommodationPost post);
    }

    private final List<AccommodationPost> posts = new ArrayList<>();
    private final OnPostActionListener listener;

    public MyPostAdapter(@NonNull OnPostActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<AccommodationPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_post, parent, false);
        return new MyPostViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class MyPostViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvAddress;
        private final TextView tvStatus;
        private final TextView btnViewPost;
        private final TextView btnEditPost;
        private final TextView btnDeletePost;
        private final MaterialButton btnToggleStatus;

        private final OnPostActionListener listener;

        MyPostViewHolder(@NonNull View itemView, @NonNull OnPostActionListener listener) {
            super(itemView);
            this.listener = listener;

            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvAddress = itemView.findViewById(R.id.tvPostAddress);
            tvStatus = itemView.findViewById(R.id.tvPostStatus);

            btnViewPost = itemView.findViewById(R.id.btnViewPost);
            btnEditPost = itemView.findViewById(R.id.btnEditPost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
            btnToggleStatus = itemView.findViewById(R.id.btnTogglePostStatus);
        }

        void bind(@NonNull AccommodationPost post) {
            Context context = itemView.getContext();

            String title = post.getTitle() != null ? post.getTitle() : context.getString(R.string.my_post_default_title);
            String rawAddress = post.getAddress() != null ? post.getAddress() : context.getString(R.string.settings_value_not_updated);
            String address = context.getString(R.string.my_post_default_address, rawAddress);

            boolean isExchanged = Constants.POST_EXCHANGED.equalsIgnoreCase(post.getStatus()) || "Đã đổi nhà".equalsIgnoreCase(post.getStatus());
            boolean isAvailable = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());

            tvTitle.setText(title);
            tvAddress.setText(address);

            if (isExchanged) {
                tvStatus.setText("Đã đổi nhà");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_info));
            } else if (isAvailable) {
                tvStatus.setText("Đang hiển thị");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.olive_primary_dark));
            } else {
                tvStatus.setText("Khác");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_sub));
            }
            if (btnToggleStatus != null) {
                btnToggleStatus.setVisibility(View.VISIBLE);
                btnToggleStatus.setText(isAvailable ? "Đánh dấu đã đổi" : "Mở lại bài đăng");
                btnToggleStatus.setOnClickListener(v -> listener.onToggleStatus(post));
            }

            if (btnViewPost != null) {
                btnViewPost.setOnClickListener(v -> listener.onViewPost(post));
            }
            if (btnEditPost != null) {
                btnEditPost.setOnClickListener(v -> listener.onEditPost(post));
            }
            if (btnDeletePost != null) {
                btnDeletePost.setOnClickListener(v -> listener.onDeletePost(post));
            }
        }
    }
}
