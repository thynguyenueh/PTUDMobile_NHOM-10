package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsViewModel extends AndroidViewModel {

    public enum NotifType { MESSAGE }

    public static class AppNotification {
        public final String title;
        public final String body;
        public final String timeFormatted;
        public final boolean isRead;
        public final NotifType type;
        public final String chatRoomId;
        public final String chatTargetUserId;
        public final long timestamp;

        public AppNotification(String title, String body, String timeFormatted, boolean isRead,
                               NotifType type, String chatRoomId, String chatTargetUserId, long timestamp) {
            this.title = title;
            this.body = body;
            this.timeFormatted = timeFormatted;
            this.isRead = isRead;
            this.type = type;
            this.chatRoomId = chatRoomId;
            this.chatTargetUserId = chatTargetUserId;
            this.timestamp = timestamp;
        }
    }

    private final MutableLiveData<UiState<List<AppNotification>>> notificationsState = new MutableLiveData<>(UiState.loading());
    private final ChatRepository chats;
    private final String userId;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        chats = new ChatRepository(application);
        userId = new SessionManager(application).getUserId();
    }

    public LiveData<UiState<List<AppNotification>>> getNotificationsState() {
        return notificationsState;
    }

    public void loadNotifications() {
        if (userId == null || userId.trim().isEmpty()) {
            notificationsState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }
        notificationsState.setValue(UiState.loading());
        List<AppNotification> collected = Collections.synchronizedList(new ArrayList<>());
        chats.getUserRooms(userId, new AppCallback<List<ChatRoom>>() {
            @Override
            public void onSuccess(List<ChatRoom> rooms) {
                if (rooms != null) {
                    for (ChatRoom room : rooms) {
                        if (room == null || room.getRoomId() == null || room.getRoomId().trim().isEmpty()
                                || room.getLastMessage() == null || room.getLastMessage().trim().isEmpty()
                                || userId.equals(room.getLastSenderId())) continue;
                        String targetUserId = null;
                        if (room.getParticipantIds() != null) {
                            for (String participantId : room.getParticipantIds()) {
                                if (participantId != null && !participantId.equals(userId)) {
                                    targetUserId = participantId;
                                    break;
                                }
                            }
                        }
                        collected.add(new AppNotification("Tin nhắn mới", room.getLastMessage(),
                                formatTime(room.getLastMessageTime()), false, NotifType.MESSAGE,
                                room.getRoomId(), targetUserId, room.getLastMessageTime()));
                    }
                }
                collected.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                notificationsState.postValue(UiState.content(new ArrayList<>(collected), UiState.SyncState.SYNCED));
            }

            @Override
            public void onError(Exception e) {
                notificationsState.postValue(UiState.error("Không thể tải thông báo. Vui lòng kiểm tra kết nối mạng."));
            }
        });
    }

    private String formatTime(long timestamp) {
        return timestamp <= 0 ? "" : android.text.format.DateFormat.getMediumDateFormat(getApplication()).format(timestamp)
                + " " + android.text.format.DateFormat.getTimeFormat(getApplication()).format(timestamp);
    }
}
