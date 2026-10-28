package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.example.studenthousing.data.model.MatchSuggestion;
import com.example.studenthousing.ui.post.DetailActivity;
import com.example.studenthousing.ui.post.PostDisplayFormatter;
import com.example.studenthousing.ui.exchange.ProposalActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
    private final List<MatchSuggestion> list;

    public SuggestionAdapter(List<MatchSuggestion> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchSuggestion item = list.get(position);
        AccommodationPost partner = item.getPartner();
        Context context = holder.itemView.getContext();

        if (partner == null) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.btnProposeSwap.setOnClickListener(null);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);

        String title = partner.getTitle() != null
                ? partner.getTitle() : context.getString(R.string.my_post_default_title);
        holder.tvPartnerTitle.setText(title);
        holder.tvPartnerAddress.setText(PostDisplayFormatter.getPublicArea(context, partner));

        String referenceTitle = item.getMine() == null ? "Bài đăng của bạn"
                : item.getMine().getTitle() == null
                ? "Bài đăng của bạn" : item.getMine().getTitle();
        holder.tvRefPost.setText("Phù hợp với: " + referenceTitle);
        holder.ivPartnerVerified.setVisibility(
                PostDisplayFormatter.isAddressVerified(partner) ? View.VISIBLE : View.GONE);

        String imageUrl = partner.getImages() != null && !partner.getImages().isEmpty()
                ? partner.getImages().get(0) : null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.ivPartnerPhoto);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.ivPartnerPhoto);
            holder.ivPartnerPhoto.setImageResource(R.drawable.bg_image_placeholder);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("post_id", partner.getPostId());
            intent.putExtra("POST_ID", partner.getPostId());
            v.getContext().startActivity(intent);
        });

        holder.btnProposeSwap.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProposalActivity.class);
            intent.putExtra("receiver_post_id", partner.getPostId());
            intent.putExtra("receiver_user_id", partner.getUserId());
            intent.putExtra("HOST_NAME", partner.getFamilyName());
            v.getContext().startActivity(intent);
        });
    }

    public void replaceItems(List<MatchSuggestion> items) {
        list.clear();
        if (items != null) list.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartnerTitle, tvPartnerAddress, tvRefPost;
        ImageView ivPartnerPhoto;
        ImageView ivPartnerVerified;
        MaterialButton btnProposeSwap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPartnerTitle = itemView.findViewById(R.id.tvPartnerTitle);
            tvPartnerAddress = itemView.findViewById(R.id.tvPartnerAddress);
            tvRefPost = itemView.findViewById(R.id.tvRefPost);
            ivPartnerPhoto = itemView.findViewById(R.id.ivPartnerPhoto);
            ivPartnerVerified = itemView.findViewById(R.id.ivPartnerVerified);
            btnProposeSwap = itemView.findViewById(R.id.btnProposeSwap);
        }
    }
}
