package com.example.studenthousing;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.databinding.ItemPhotoBinding;

import java.util.List;

/** Adapter cho ViewPager2 hiển thị ảnh căn hộ. */
public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    private final List<Integer> photoResIds;

    public PhotoPagerAdapter(@NonNull List<Integer> photoResIds) {
        this.photoResIds = photoResIds;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhotoBinding binding =
                ItemPhotoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PhotoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(photoResIds.get(position));
    }

    @Override
    public int getItemCount() {
        return photoResIds.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;

        PhotoViewHolder(@NonNull ItemPhotoBinding binding) {
            super(binding.getRoot());
            this.imageView = binding.getRoot();
        }

        void bind(int photoResId) {
            imageView.setImageResource(photoResId);
        }
    }
}
