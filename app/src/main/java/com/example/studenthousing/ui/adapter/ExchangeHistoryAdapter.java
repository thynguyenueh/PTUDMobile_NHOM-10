package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeHistoryAdapter extends RecyclerView.Adapter<ExchangeHistoryAdapter.ExchangeViewHolder> {

    public interface OnExchangeActionListener {
        void onAcceptExchange(@NonNull ExchangeRequest request);
        void onRejectExchange(@NonNull ExchangeRequest request);
        void onReviewPartner(@NonNull ExchangeRequest request);
        void onViewAgreement(@NonNull ExchangeRequest request);
    }

    private final List<ExchangeRequest> requests = new ArrayList<>();
    private final OnExchangeActionListener listener;
    private final String currentUserId;

    private final Map<String, String> userNameCache = new HashMap<>();
    private final Map<String, String> postTitleCache = new HashMap<>();

    public ExchangeHistoryAdapter(@NonNull OnExchangeActionListener listener, @NonNull String currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setCache(Map<String, String> userNames, Map<String, String> postTitles) {
        userNameCache.clear();
        postTitleCache.clear();
        if (userNames != null) userNameCache.putAll(userNames);
        if (postTitles != null) postTitleCache.putAll(postTitles);
    }

    public void submitList(@NonNull List<ExchangeRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExchangeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exchange_history, parent, false);
        return new ExchangeViewHolder(view, listener, currentUserId);
    }

    @Override
    public void onBindViewHolder(@NonNull ExchangeViewHolder holder, int position) {
        holder.bind(requests.get(position), userNameCache, postTitleCache);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ExchangeViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvPartner;
        private final TextView tvStatus;
        private final TextView tvPeriod;
        private final TextView tvMessage;
        private final View layoutPendingActions;
        private final View btnAcceptExchange;
        private final View btnRejectExchange;
        private final View btnReviewPartner;
        private final View btnViewAgreement;

        private final OnExchangeActionListener listener;
        private final String currentUserId;

        ExchangeViewHolder(@NonNull View itemView, @NonNull OnExchangeActionListener listener, @NonNull String currentUserId) {
            super(itemView);
            this.listener = listener;
            this.currentUserId = currentUserId;

            tvTitle = itemView.findViewById(R.id.tvExchangeTitle);
            tvPartner = itemView.findViewById(R.id.tvExchangePartner);
            tvStatus = itemView.findViewById(R.id.tvExchangeStatus);
            tvPeriod = itemView.findViewById(R.id.tvExchangePeriod);
            tvMessage = itemView.findViewById(R.id.tvExchangeMessage);

            layoutPendingActions = itemView.findViewById(R.id.layoutPendingActions);
            btnAcceptExchange = itemView.findViewById(R.id.btnAcceptExchange);
            btnRejectExchange = itemView.findViewById(R.id.btnRejectExchange);
            btnReviewPartner = itemView.findViewById(R.id.btnReviewPartner);
            btnViewAgreement = itemView.findViewById(R.id.btnViewAgreement);
        }

        void bind(@NonNull ExchangeRequest req, Map<String, String> userNames, Map<String, String> postTitles) {
            Context context = itemView.getContext();
            String rawStatus = req.getStatus() != null ? req.getStatus().toUpperCase() : Constants.REQ_PENDING;
            String displayStatus;
            switch (rawStatus) {
                case Constants.REQ_EXCHANGING:
                    displayStatus = "Đang trao đổi";
                    tvStatus.setBackgroundResource(R.drawable.bg_verified);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.olive_accent));
                    break;
                case Constants.REQ_COMPLETED:
                    displayStatus = "Hoàn thành";
                    tvStatus.setBackgroundResource(R.drawable.bg_verified);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.olive_accent));
                    break;
                case Constants.REQ_REJECTED:
                    displayStatus = "Bị từ chối";
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_danger));
                    break;
                case Constants.REQ_CANCELLED:
                    displayStatus = "Đã huỷ";
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_danger));
                    break;
                case Constants.REQ_WITHDRAWN:
                    displayStatus = "Đã rút lại";
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_danger));
                    break;
                case Constants.REQ_EXPIRED:
                    displayStatus = "Hết hạn";
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_red);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_danger));
                    break;
                default:
                    displayStatus = "Đang chờ phản hồi";
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_warning);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
                    break;
            }

            boolean isInbound = currentUserId.equals(req.getReceiverId());
            String partnerId = isInbound ? req.getSenderId() : req.getReceiverId();
            String postIdForDisplay = isInbound ? req.getSenderPostId() : req.getReceiverPostId();

            String postTitle = postTitles.get(postIdForDisplay);
            if (postTitle == null) postTitle = "Đề xuất trao đổi";

            String partnerName = userNames.get(partnerId);
            if (partnerName == null) partnerName = partnerId != null ? partnerId : "Đối tác đổi nhà";

            String period = "Thời gian đổi: " + (req.getExchangePeriod() != null ? req.getExchangePeriod() : "Học kỳ 1");
            String message = req.getMessage() != null ? req.getMessage() : "Thỏa thuận đổi nhà 2 chiều.";

            tvStatus.setText(displayStatus);
            tvPeriod.setText(period);
            tvMessage.setText(message);
            tvTitle.setText(postTitle);
            tvPartner.setText((isInbound ? "Người gửi: " : "Đối tác: ") + partnerName);

            boolean isDoneOrActive = Constants.REQ_EXCHANGING.equalsIgnoreCase(rawStatus) || Constants.REQ_COMPLETED.equalsIgnoreCase(rawStatus);
            boolean canAct = isInbound && Constants.REQ_PENDING.equalsIgnoreCase(rawStatus);
            if (layoutPendingActions != null) {
                layoutPendingActions.setVisibility(canAct || isDoneOrActive ? View.VISIBLE : View.GONE);
            }
            if (btnReviewPartner != null) {
                btnReviewPartner.setVisibility(isDoneOrActive ? View.VISIBLE : View.GONE);
            }
            if (btnViewAgreement != null) {
                btnViewAgreement.setVisibility(isDoneOrActive ? View.VISIBLE : View.GONE);
            }

            if (btnAcceptExchange != null) {
                btnAcceptExchange.setOnClickListener(v -> listener.onAcceptExchange(req));
            }
            if (btnRejectExchange != null) {
                btnRejectExchange.setOnClickListener(v -> listener.onRejectExchange(req));
            }
            if (btnReviewPartner != null) {
                btnReviewPartner.setOnClickListener(v -> listener.onReviewPartner(req));
            }
            if (btnViewAgreement != null) {
                btnViewAgreement.setOnClickListener(v -> listener.onViewAgreement(req));
            }
        }
    }
}
