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

    public interface OnAddressClickListener {
        void onAddressClick(VerifiedAddress address);
    }

    private final List<VerifiedAddress> list = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnAddressClickListener listener;

    public void setOnAddressClickListener(OnAddressClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<VerifiedAddress> items) {
        list.clear();
        if (items != null) {
            list.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_verified_address, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VerifiedAddress item = list.get(position);
        holder.tvFullAddress.setText(item.getFullAddress());
        if (item.getVerifiedAt() > 0) {
            holder.tvVerifiedDate.setText(dateFormat.format(new Date(item.getVerifiedAt())));
            holder.tvVerifiedDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvVerifiedDate.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAddressClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFullAddress;
        final TextView tvVerifiedDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullAddress = itemView.findViewById(R.id.tvFullAddress);
            tvVerifiedDate = itemView.findViewById(R.id.tvVerifiedDate);
        }
    }
}
