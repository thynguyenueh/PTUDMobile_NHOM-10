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
import com.example.studenthousing.data.loader.UtilitiesDataLoader;
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.Utility;
import com.example.studenthousing.ui.post.PostDisplayFormatter;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<Post> postList;
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public PostAdapter(List<Post> postList, OnPostClickListener listener) {
        this.postList = postList != null ? postList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Post> newList) {
        this.postList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
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
        Post post = postList.get(position);
        if (post == null) return;
        Context context = holder.itemView.getContext();

        // 1. Tiêu đề
        holder.tvTitle.setText(post.getTitle() != null
                ? post.getTitle()
                : context.getString(R.string.my_post_default_title));

        // 2. Địa chỉ công khai (ưu tiên address đầy đủ - giống DetailActivity)
        holder.tvAddress.setText(buildPublicAddress(post));

        // 3. Khoảng cách tới trường
        String campusSummary = PostDisplayFormatter.getNearestCampusSummary(context, post);
        holder.tvDistance.setText(campusSummary == null ? "" : campusSummary);
        holder.tvDistance.setVisibility(campusSummary == null ? View.GONE : View.VISIBLE);

        // 4. Tiện ích — RESOLVE CODE SANG LABEL
        String utilitiesSummary = buildUtilitiesSummary(context, post);
        holder.tvUtilities.setText(utilitiesSummary);
        holder.tvUtilities.setVisibility(utilitiesSummary.isEmpty() ? View.GONE : View.VISIBLE);

        // 5. Thời gian
        if (holder.tvTime != null) {
            String timeStr = com.example.studenthousing.util.TimeFormatter.relative(context, post.getCreatedAt());
            holder.tvTime.setText(timeStr);
            holder.tvTime.setVisibility(timeStr.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // 6. Trạng thái
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

        // 7. Badge xác thực
        if (holder.ivVerified != null) {
            holder.ivVerified.setVisibility(
                    PostDisplayFormatter.isAddressVerified(post) ? View.VISIBLE : View.GONE);
        }

        // 8. Ảnh đại diện
        String imageUrl = post.getImages() != null && !post.getImages().isEmpty()
                ? post.getImages().get(0) : null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.ivPhoto);
        } else {
            Glide.with(context).clear(holder.ivPhoto);
            holder.ivPhoto.setImageResource(R.drawable.bg_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });
    }

    /**
     * ✅ FIX: Ưu tiên address đầy đủ (giống DetailActivity).
     * Chỉ fallback ward+city nếu address rỗng.
     */
    private String buildPublicAddress(Post post) {
        String full = post.getAddress() != null ? post.getAddress().trim() : "";
        String ward = post.getWardName() != null ? post.getWardName().trim() : "";
        String city = post.getCityName() != null ? post.getCityName().trim() : "";

        // 🔑 Ưu tiên address đầy đủ (giống hệt Detail đang hiển thị)
        if (!full.isEmpty()) {
            return full;
        }

        // Fallback nếu address rỗng
        if (!ward.isEmpty() && !city.isEmpty()) return ward + ", " + city;
        if (!ward.isEmpty()) return ward;
        if (!city.isEmpty()) return city;

        return "Đang ẩn địa chỉ";
    }

    /**
     * Resolve danh sách tiện ích:
     * - Nếu phần tử là mã code (VD: "WIFI", "AIR_CONDITIONER") → tìm label tương ứng.
     * - Nếu phần tử đã là label tiếng Việt → giữ nguyên.
     * - Hiển thị tối đa 2 tiện ích + "(+N)" nếu còn.
     */
    private String buildUtilitiesSummary(Context context, Post post) {
        List<String> utilities = post.getUtilities();
        if (utilities == null || utilities.isEmpty()) return "";

        List<String> labels = new ArrayList<>();
        for (String raw : utilities) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String trimmed = raw.trim();

            Utility utilObj = UtilitiesDataLoader.getByCode(context, trimmed);
            if (utilObj != null && utilObj.getLabel() != null && !utilObj.getLabel().isEmpty()) {
                labels.add(utilObj.getLabel());
            } else {
                labels.add(trimmed);
            }
        }

        if (labels.isEmpty()) return "";

        if (labels.size() > 2) {
            return String.join(" · ", labels.subList(0, 2)) + " (+" + (labels.size() - 2) + ")";
        }
        return String.join(" · ", labels);
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
            tvUtilities = itemView.findViewById(R.id.tvHouseUtilities);
            tvTime = itemView.findViewById(R.id.tvHouseTime);
            ivVerified = itemView.findViewById(R.id.ivHouseVerified);
        }
    }
}