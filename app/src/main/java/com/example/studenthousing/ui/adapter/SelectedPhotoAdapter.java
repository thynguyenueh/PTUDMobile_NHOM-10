package com.example.studenthousing.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;

import java.util.ArrayList;
import java.util.List;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.ViewHolder> {

    public interface OnPhotoRemoveListener {
        void onRemove(int position);
    }

    private final List<Uri> uris = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final OnPhotoRemoveListener listener;
    private boolean removeEnabled = true; // Mặc định cho phép xóa

    // ✅ Constructor 1: Dùng cho KYC (không cần truyền list ban đầu)
    public SelectedPhotoAdapter(OnPhotoRemoveListener listener) {
        this.listener = listener;
    }

    // ✅ Constructor 2: Dùng cho CreatePostActivity (truyền list ban đầu)
    public SelectedPhotoAdapter(List<Uri> initialUris, OnPhotoRemoveListener listener) {
        if (initialUris != null) {
            uris.addAll(initialUris);
            for (int i = 0; i < initialUris.size(); i++) {
                labels.add("");
            }
        }
        this.listener = listener;
    }

    public void setData(List<Uri> newUris, List<String> newLabels) {
        uris.clear();
        labels.clear();
        if (newUris != null) uris.addAll(newUris);
        if (newLabels != null) labels.addAll(newLabels);
        while (labels.size() < uris.size()) labels.add("");
        notifyDataSetChanged();
    }

    public void setRemoveEnabled(boolean enabled) {
        this.removeEnabled = enabled;
        notifyDataSetChanged();
    }

    public List<Uri> getUris() {
        return new ArrayList<>(uris);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = uris.get(position);
        String label = position < labels.size() ? labels.get(position) : "";

        Glide.with(holder.itemView.getContext())
                .load(uri)
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(holder.ivPhoto);

        if (label != null && !label.isEmpty()) {
            holder.tvPhotoLabel.setText(label);
            holder.tvPhotoLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvPhotoLabel.setVisibility(View.GONE);
        }

        // Hiển thị hoặc ẩn nút X
        holder.btnRemovePhoto.setVisibility(removeEnabled ? View.VISIBLE : View.GONE);

        if (removeEnabled) {
            holder.btnRemovePhoto.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRemove(pos);
                }
            });
        } else {
            holder.btnRemovePhoto.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnRemovePhoto;
        TextView tvPhotoLabel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            btnRemovePhoto = itemView.findViewById(R.id.btnRemovePhoto);
            tvPhotoLabel = itemView.findViewById(R.id.tvPhotoLabel);
        }
    }
}