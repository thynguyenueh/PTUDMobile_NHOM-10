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
import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.ui.post.PostDisplayFormatter;

import java.util.ArrayList;
import java.util.List;

public class SavedHouseAdapter extends RecyclerView.Adapter<SavedHouseAdapter.SavedHouseViewHolder> {
    public interface OnSavedHouseActionListener {
        void onRemoveSaved(int position, SavedHouseItem item);
        void onViewDetail(int position, SavedHouseItem item);
    }

    public static class SavedHouseItem {
        public final String postId, title, address, imageUrl, campusSummary;
        public final int statusLabel;
        public final boolean addressVerified;

        public SavedHouseItem(String postId, String title, String address, String imageUrl,
                              String campusSummary, int statusLabel, boolean addressVerified) {
            this.postId = postId;
            this.title = title;
            this.address = address;
            this.imageUrl = imageUrl;
            this.campusSummary = campusSummary;
            this.statusLabel = statusLabel;
            this.addressVerified = addressVerified;
        }

        public static SavedHouseItem fromPost(Context context, Post post) {
            String title = post.getTitle() != null ? post.getTitle() : context.getString(R.string.my_post_default_title);
            String address = PostDisplayFormatter.getPublicArea(context, post);
            return new SavedHouseItem(post.getPostId(), title, address, firstImage(post),
                    PostDisplayFormatter.getNearestCampusSummary(context, post),
                    PostDisplayFormatter.getCompactStatusLabel(post),
                    PostDisplayFormatter.isAddressVerified(post));
        }

        private static String firstImage(Post post) {
            return post.getImages() != null && !post.getImages().isEmpty() ? post.getImages().get(0) : null;
        }
    }

    private final List<SavedHouseItem> items = new ArrayList<>();
    private final OnSavedHouseActionListener listener;

    public SavedHouseAdapter(@NonNull OnSavedHouseActionListener listener) { this.listener = listener; }
    public void submitList(@NonNull List<SavedHouseItem> newItems) { items.clear(); items.addAll(newItems); notifyDataSetChanged(); }
    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }
    @Override public int getItemCount() { return items.size(); }
    @NonNull @Override public SavedHouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SavedHouseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_house, parent, false), listener);
    }
    @Override public void onBindViewHolder(@NonNull SavedHouseViewHolder holder, int position) { holder.bind(items.get(position)); }

    public static class SavedHouseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvAddress;
        private final TextView tvStatus, tvDistance;
        private final ImageView ivSavedPhoto;
        private final ImageView ivVerified;
        private final View btnRemoveSaved;
        private final OnSavedHouseActionListener listener;

        SavedHouseViewHolder(@NonNull View itemView, @NonNull OnSavedHouseActionListener listener) {
            super(itemView);
            this.listener = listener;
            tvTitle = itemView.findViewById(R.id.tvSavedTitle);
            tvAddress = itemView.findViewById(R.id.tvSavedAddress);
            tvStatus = itemView.findViewById(R.id.tvSavedStatus);
            tvDistance = itemView.findViewById(R.id.tvSavedDistance);
            ivSavedPhoto = itemView.findViewById(R.id.ivSavedPhoto);
            ivVerified = itemView.findViewById(R.id.tvSavedVerified);
            btnRemoveSaved = itemView.findViewById(R.id.btnRemoveSaved);
        }

        void bind(@NonNull SavedHouseItem item) {
            tvTitle.setText(item.title);
            tvAddress.setText(item.address);
            tvStatus.setVisibility(item.statusLabel == 0 ? View.GONE : View.VISIBLE);
            if (item.statusLabel != 0) tvStatus.setText(item.statusLabel);
            ivVerified.setVisibility(item.addressVerified ? View.VISIBLE : View.GONE);
            tvDistance.setText(item.campusSummary == null ? "" : item.campusSummary);
            tvDistance.setVisibility(item.campusSummary == null ? View.GONE : View.VISIBLE);
            if (ivSavedPhoto != null) {
                int[] placeholders = {R.drawable.bg_image_placeholder};
                int p = Math.max(0, getBindingAdapterPosition()) % placeholders.length;
                if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
                    Glide.with(itemView.getContext()).load(item.imageUrl).placeholder(placeholders[p]).error(placeholders[p]).into(ivSavedPhoto);
                } else {
                    Glide.with(itemView.getContext()).clear(ivSavedPhoto);
                    ivSavedPhoto.setImageResource(placeholders[p]);
                }
            }
            if (btnRemoveSaved != null) btnRemoveSaved.setOnClickListener(v -> listener.onRemoveSaved(getAbsoluteAdapterPosition(), item));
            itemView.setOnClickListener(v -> listener.onViewDetail(getAbsoluteAdapterPosition(), item));
        }
    }
}
