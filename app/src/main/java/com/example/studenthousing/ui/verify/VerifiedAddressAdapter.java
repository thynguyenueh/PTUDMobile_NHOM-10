package com.example.studenthousing.ui.verify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.VerifiedAddress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VerifiedAddressAdapter extends RecyclerView.Adapter<VerifiedAddressAdapter.ViewHolder> {
    private final List<VerifiedAddress> list = new ArrayList<>();
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(VerifiedAddress address);
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void submitList(List<VerifiedAddress> items) {
        list.clear();
        if (items != null) list.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verified_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VerifiedAddress item = list.get(position);
        if (item == null) return;
        holder.tvFullAddress.setText(item.getFullAddress() != null ? item.getFullAddress() : "Địa chỉ đã xác thực");
        if (item.getVerifiedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvVerifiedDate.setText("Đã xác thực: " + sdf.format(new Date(item.getVerifiedAt())));
        } else {
            holder.tvVerifiedDate.setText("Đã xác thực");
        }
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullAddress, tvVerifiedDate;
        android.widget.ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullAddress = itemView.findViewById(R.id.tvFullAddress);
            tvVerifiedDate = itemView.findViewById(R.id.tvVerifiedDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
