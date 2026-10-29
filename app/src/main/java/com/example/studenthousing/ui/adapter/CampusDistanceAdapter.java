package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.repository.CampusRepository;

import java.util.List;
import java.util.Locale;

public class CampusDistanceAdapter extends RecyclerView.Adapter<CampusDistanceAdapter.ViewHolder> {

    private final Context context;
    private final List<NearbyCampus> items;

    public CampusDistanceAdapter(Context context, List<NearbyCampus> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_campus_distance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyCampus nearby = items.get(position);
        if (nearby == null) return;

        Campus campus = CampusRepository.getInstance(context).findByCode(nearby.getCampusId());
        String name = campus != null ? campus.getDisplayName() : nearby.getCampusId();
        String district = campus != null ? campus.getDistrict() : "";

        if (holder.tvName != null) holder.tvName.setText(name);
        if (holder.tvDistrict != null) {
            if (district != null && !district.trim().isEmpty()) {
                holder.tvDistrict.setText(district);
                holder.tvDistrict.setVisibility(View.VISIBLE);
            } else {
                holder.tvDistrict.setVisibility(View.GONE);
            }
        }

        String distStr = String.format(Locale.getDefault(), "%.1f km", nearby.getKm());
        if (holder.tvDistance != null) holder.tvDistance.setText(distStr);
        if (holder.tvType != null) {
            holder.tvType.setText(nearby.isEstimate() ? "Đường chim bay / Ước lượng" : "Đường đi thực tế");
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDistrict, tvDistance, tvType;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCampusName);
            tvDistrict = itemView.findViewById(R.id.tvCampusDistrict);
            tvDistance = itemView.findViewById(R.id.tvCampusDistance);
            tvType = itemView.findViewById(R.id.tvDistanceType);
        }
    }
}
