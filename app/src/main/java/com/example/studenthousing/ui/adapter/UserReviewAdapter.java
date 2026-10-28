package com.example.studenthousing.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.model.Review;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserReviewAdapter extends RecyclerView.Adapter<UserReviewAdapter.ReviewViewHolder> {

    private final List<Review> reviews = new ArrayList<>();

    public void submitList(@NonNull List<Review> newReviews) {
        reviews.clear();
        reviews.addAll(newReviews);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView tvStars;
        private final TextView tvComment;
        private final View scrollAttachments;
        private final LinearLayout attachmentsContainer;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvReviewerAvatar);
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvStars = itemView.findViewById(R.id.tvReviewRatingStars);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            scrollAttachments = itemView.findViewById(R.id.scrollReviewAttachments);
            attachmentsContainer = itemView.findViewById(R.id.reviewAttachmentsContainer);
        }

        void bind(@NonNull Review r) {
            String name = r.getReviewerName();
            if (name == null || name.isEmpty() || name.startsWith("Đối tác ")) {
                if (r.getReviewerId() != null) {
                    com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(itemView.getContext());
                    com.example.studenthousing.data.model.User user = userDao.getById(r.getReviewerId());
                    if (user != null && user.getFamilyName() != null && !user.getFamilyName().isEmpty()) {
                        name = user.getFamilyName();
                    }
                }
            }
            if (name == null || name.isEmpty() || name.startsWith("Đối tác ")) {
                name = "Hộ gia đình đối tác";
            }
            tvName.setText(name);
            tvAvatar.setText(name.substring(0, 1).toUpperCase());

            int rating = (int) Math.round(r.getRating());
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < rating ? "★" : "☆");
            }
            tvStars.setText(stars.toString());

            String dateStr;
            if (r.getCreatedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                dateStr = sdf.format(new Date(r.getCreatedAt()));
            } else {
                dateStr = "Gần đây";
            }
            tvDate.setText(dateStr + " · Đối tác đổi nhà");

            tvComment.setText(r.getComment() != null ? r.getComment() : "");

            List<String> images = r.getImageUrls() != null ? r.getImageUrls() : new ArrayList<>();
            if (scrollAttachments != null) {
                scrollAttachments.setVisibility(images.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (attachmentsContainer != null) {
                attachmentsContainer.removeAllViews();
                for (String imgUrl : images) {
                    if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                        ImageView iv = new ImageView(itemView.getContext());
                        int size = (int) (80 * itemView.getContext().getResources().getDisplayMetrics().density);
                        int margin = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                        lp.setMarginEnd(margin);
                        iv.setLayoutParams(lp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setBackgroundResource(R.drawable.bg_image_placeholder);

                        Glide.with(itemView.getContext())
                                .load(imgUrl)
                                .placeholder(R.drawable.bg_image_placeholder)
                                .error(R.drawable.bg_image_placeholder)
                                .into(iv);

                        attachmentsContainer.addView(iv);
                    }
                }
            }
        }
    }
}
