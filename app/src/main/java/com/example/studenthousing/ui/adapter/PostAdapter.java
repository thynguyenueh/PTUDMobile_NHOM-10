package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.ui.post.PostDisplayFormatter;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<AccommodationPost> postList;
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(AccommodationPost post);
    }

    public PostAdapter(List<AccommodationPost> postList, OnPostClickListener listener) {
        this.postList = postList != null ? postList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<AccommodationPost> newList) {
        this.postList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_house, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AccommodationPost post = postList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(post.getTitle() != null ? post.getTitle() : context.getString(R.string.my_post_default_title));
        holder.tvAddress.setText(PostDisplayFormatter.getPublicArea(context, post));

        String campusSummary = PostDisplayFormatter.getNearestCampusSummary(context, post);
        holder.tvDistance.setText(campusSummary == null ? "" : campusSummary);
        holder.tvDistance.setVisibility(campusSummary == null ? View.GONE : View.VISIBLE);

        String utilitiesSummary = PostDisplayFormatter.getUtilitiesSummary(context, post);
        holder.tvUtilities.setText(utilitiesSummary == null ? "" : utilitiesSummary);
        holder.tvUtilities.setVisibility(utilitiesSummary == null ? View.GONE : View.VISIBLE);

        if (holder.tvTime != null) {
            String timeStr = com.example.studenthousing.util.TimeFormatter.relative(context, post.getCreatedAt());
            holder.tvTime.setText(timeStr);
            holder.tvTime.setVisibility(timeStr.isEmpty() ? View.GONE : View.VISIBLE);
        }

        boolean isAvailable = Constants.POST_AVAILABLE.equalsIgnoreCase(post.getStatus());
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(isAvailable ? "Sẵn sàng đổi" : "Đã đổi");
            holder.tvStatus.setBackgroundResource(isAvailable
                    ? R.drawable.bg_badge_verified
                    : R.drawable.bg_badge_distance);
            holder.tvStatus.setTextColor(context.getColor(isAvailable
                    ? R.color.olive_primary_dark
                    : R.color.text_sub));
            holder.tvStatus.setVisibility(View.VISIBLE);
        }
        if (holder.ivVerified != null) {
            holder.ivVerified.setVisibility(
                    PostDisplayFormatter.isAddressVerified(post) ? View.VISIBLE : View.GONE);
        }

        String imageUrl = post.getImages() != null && !post.getImages().isEmpty()
                ? post.getImages().get(0) : null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.ivPhoto);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.ivPhoto);
            holder.ivPhoto.setImageResource(R.drawable.bg_image_placeholder);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle, tvAddress, tvDistance, tvStatus, tvUtilities, tvTime;
        ImageView ivVerified;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivHousePhoto);
            tvTitle = itemView.findViewById(R.id.tvHouseTitle);
            tvAddress = itemView.findViewById(R.id.tvHouseAddress);
            tvDistance = itemView.findViewById(R.id.tvHouseDistance);
            tvStatus = itemView.findViewById(R.id.tvPostStatus);
            tvUtilities = itemView.findViewById(R.id.tvHouseUtilities);
            tvTime = itemView.findViewById(R.id.tvHouseTime);
            ivVerified = itemView.findViewById(R.id.ivHouseVerified);
        }
    }
}
