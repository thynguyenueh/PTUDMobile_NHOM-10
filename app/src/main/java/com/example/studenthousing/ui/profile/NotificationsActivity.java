package com.example.studenthousing.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.ui.viewmodel.NotificationsViewModel;
import com.example.studenthousing.ui.viewmodel.NotificationsViewModel.AppNotification;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private NotificationsViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvNotifications = findViewById(R.id.rvNotifications);
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
            adapter = new NotificationAdapter(new ArrayList<>());
            rvNotifications.setAdapter(adapter);
        }

        TextView stateView = findViewById(R.id.tvNotificationState);
        findViewById(R.id.btnRefreshNotifications).setOnClickListener(v -> viewModel.loadNotifications());
        viewModel.getNotificationsState().observe(this, state -> {
            renderNotifications(state);
            if (stateView != null) {
                if (state == null || state.getScreenState() == UiState.ScreenState.LOADING) {
                    stateView.setText(R.string.notifications_loading);
                    stateView.setVisibility(View.VISIBLE);
                } else if (state.getScreenState() == UiState.ScreenState.ERROR) {
                    stateView.setText(state.getErrorMessage());
                    stateView.setVisibility(View.VISIBLE);
                } else if (state.getContent() == null || state.getContent().isEmpty()) {
                    stateView.setText(R.string.notifications_empty);
                    stateView.setVisibility(View.VISIBLE);
                } else {
                    stateView.setVisibility(View.GONE);
                }
            }
        });
        viewModel.loadNotifications();
    }

    private void renderNotifications(UiState<List<AppNotification>> state) {
        if (state == null) return;
        if (adapter != null) {
            adapter.updateItems(state.getContent() == null ? new ArrayList<>() : state.getContent());
        }
    }

    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        private List<AppNotification> items;

        NotificationAdapter(List<AppNotification> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        public void updateItems(List<AppNotification> newItems) {
            this.items = newItems != null ? newItems : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppNotification item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.body);
            holder.tvTime.setText(item.timeFormatted);

            int iconRes = R.drawable.ic_notifications;
            int tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.primary);

            holder.ivIcon.setImageResource(iconRes);
            holder.ivIcon.setColorFilter(tintColor);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;
            ImageView ivIcon;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvMessage = itemView.findViewById(R.id.tvNotifMessage);
                tvTime = itemView.findViewById(R.id.tvNotifTime);
                ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            }
        }
    }
}
