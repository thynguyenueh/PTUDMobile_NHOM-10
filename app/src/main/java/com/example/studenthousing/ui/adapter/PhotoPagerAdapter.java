package com.example.studenthousing.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.databinding.ItemPhotoBinding;

import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    private final List<String> imageUrls;
    private final OnPhotoClickListener listener;

    public PhotoPagerAdapter(@NonNull List<String> imageUrls) {
        this(imageUrls, null);
    }

    public PhotoPagerAdapter(@NonNull List<String> imageUrls, OnPhotoClickListener listener) {
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhotoBinding binding =
                ItemPhotoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PhotoViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(imageUrls.get(position), position);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final OnPhotoClickListener listener;

        PhotoViewHolder(@NonNull ItemPhotoBinding binding, OnPhotoClickListener listener) {
            super(binding.getRoot());
            this.imageView = binding.getRoot();
            this.listener = listener;
        }

        void bind(String imageUrl, int position) {
            if (listener != null) {
                imageView.setOnClickListener(v -> listener.onPhotoClick(position));
            } else {
                imageView.setOnClickListener(null);
            }

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    int resId = Integer.parseInt(imageUrl.trim());
                    imageView.setImageResource(resId);
                    return;
                } catch (NumberFormatException ignored) {}

                Glide.with(imageView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.bg_image_placeholder);
            }
        }
    }
}
