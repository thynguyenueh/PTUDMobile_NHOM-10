package com.example.homeexchange;

import com.example.studenthousing.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.databinding.ItemHouseBinding;

import java.util.ArrayList;
import java.util.List;

/** Adapter danh sách nhà nằm ngang ở trang chủ. */
public class HouseAdapter extends RecyclerView.Adapter<HouseAdapter.HouseViewHolder> {

    public interface OnHouseClickListener {
        void onHouseClick(@NonNull House house);
    }

    private final List<House> houses = new ArrayList<>();
    private final OnHouseClickListener listener;

    public HouseAdapter(@NonNull OnHouseClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<House> newHouses) {
        houses.clear();
        houses.addAll(newHouses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHouseBinding binding =
                ItemHouseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new HouseViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HouseViewHolder holder, int position) {
        holder.bind(houses.get(position));
    }

    @Override
    public int getItemCount() {
        return houses.size();
    }

    static class HouseViewHolder extends RecyclerView.ViewHolder {

        private final ItemHouseBinding binding;
        private final OnHouseClickListener listener;

        HouseViewHolder(@NonNull ItemHouseBinding binding, @NonNull OnHouseClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(@NonNull House house) {
            Context context = binding.getRoot().getContext();
            binding.ivHousePhoto.setImageResource(house.getPhotoRes());
            binding.tvHouseTitle.setText(house.getTitle());
            binding.tvHouseAddress.setText(house.getAddress());
            binding.tvHouseDistance.setText(
                    context.getString(R.string.distance_km_format, house.getDistanceKm()));
            binding.getRoot().setOnClickListener(v -> listener.onHouseClick(house));
        }
    }
}
