package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends AndroidViewModel {

    public enum NotifType { MESSAGE }

    public static class AppNotification {
        public final String title;
        public final String body;
        public final String timeFormatted;
        public final boolean isRead;
        public final NotifType type;
        public final String exchangeRequestId;
        public final String chatRoomId;
        public final String chatTargetUserId;
        public final long timestamp;

        public AppNotification(String title, String body, String timeFormatted, boolean isRead,
                               NotifType type, String exchangeRequestId, String chatRoomId,
                               String chatTargetUserId, long timestamp) {
            this.title = title;
            this.body = body;
            this.timeFormatted = timeFormatted;
            this.isRead = isRead;
            this.type = type;
            this.exchangeRequestId = exchangeRequestId;
            this.chatRoomId = chatRoomId;
            this.chatTargetUserId = chatTargetUserId;
            this.timestamp = timestamp;
        }
    }

    private final MutableLiveData<UiState<List<AppNotification>>> notificationsState = new MutableLiveData<>(UiState.loading());

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState<List<AppNotification>>> getNotificationsState() {
        return notificationsState;
    }

    public void loadNotifications() {
        notificationsState.setValue(UiState.content(new ArrayList<>(), UiState.SyncState.SYNCED));
    }
}
