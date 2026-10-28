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

import java.util.List;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.ViewHolder> {

    public interface OnPhotoRemoveListener {
        void onRemove(int position);
    }

    private final List<Uri> photoUris;
    private final OnPhotoRemoveListener removeListener;

    public SelectedPhotoAdapter(List<Uri> photoUris, OnPhotoRemoveListener removeListener) {
        this.photoUris = photoUris;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = photoUris.get(position);
        if (holder.ivPhoto != null) {
            Glide.with(holder.itemView.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.ivPhoto);
        }

        if (holder.tvCoverBadge != null) {
            holder.tvCoverBadge.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        }

        if (holder.btnRemovePhoto != null) {
            holder.btnRemovePhoto.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && removeListener != null) {
                    removeListener.onRemove(adapterPos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return photoUris != null ? photoUris.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvCoverBadge;
        ImageView btnRemovePhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvCoverBadge = itemView.findViewById(R.id.tvCoverBadge);
            btnRemovePhoto = itemView.findViewById(R.id.btnRemovePhoto);
        }
    }
}
