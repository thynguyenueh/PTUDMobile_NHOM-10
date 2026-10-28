package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationsViewModel extends AndroidViewModel {

    public enum NotifType { EXCHANGE, MESSAGE }

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
    private final ExchangeRepository exchanges;
    private final ChatRepository chats;
    private final String userId;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        exchanges = new ExchangeRepository(application);
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
        AtomicInteger pending = new AtomicInteger(2);
        AtomicInteger successes = new AtomicInteger(0);
        AppCallback<List<ExchangeRequest>> exchangeCallback = new AppCallback<List<ExchangeRequest>>() {
            @Override public void onSuccess(List<ExchangeRequest> requests) {
                if (requests != null) for (ExchangeRequest request : requests) {
                    if (request == null || request.getRequestId() == null) continue;
                    String status = request.getStatus() == null ? Constants.REQ_PENDING : request.getStatus();
                    if (!Constants.REQ_PENDING.equalsIgnoreCase(status)) continue;
                    String text = request.getMessage();
                    collected.add(new AppNotification("Yêu cầu đổi nhà mới", text == null || text.trim().isEmpty() ? "Bạn nhận được một đề xuất đổi nhà." : text,
                            formatTime(request.getCreatedAt()), false, NotifType.EXCHANGE,
                            request.getRequestId(), null, null, request.getCreatedAt()));
                }
                successes.incrementAndGet(); finishSource(pending, successes, collected);
            }
            @Override public void onError(Exception e) { finishSource(pending, successes, collected); }
        };
        AppCallback<List<ChatRoom>> roomCallback = new AppCallback<List<ChatRoom>>() {
            @Override public void onSuccess(List<ChatRoom> rooms) {
                if (rooms != null) for (ChatRoom room : rooms) {
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
                            null, room.getRoomId(), targetUserId, room.getLastMessageTime()));
                }
                successes.incrementAndGet(); finishSource(pending, successes, collected);
            }
            @Override public void onError(Exception e) { finishSource(pending, successes, collected); }
        };
        exchanges.getByReceiver(userId, exchangeCallback);
        chats.getUserRooms(userId, roomCallback);
    }

    private void finishSource(AtomicInteger pending, AtomicInteger successes, List<AppNotification> items) {
        if (pending.decrementAndGet() != 0) return;
        items.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        if (successes.get() < 2 && !items.isEmpty()) {
            notificationsState.postValue(UiState.content(new ArrayList<>(items), UiState.SyncState.SYNC_ERROR));
            return;
        }
        if (successes.get() == 0) {
            notificationsState.postValue(UiState.error("Không thể tải thông báo. Vui lòng kiểm tra kết nối mạng."));
            return;
        }
        notificationsState.postValue(UiState.content(new ArrayList<>(items), UiState.SyncState.SYNCED));
    }

    private String formatTime(long timestamp) {
        return timestamp <= 0 ? "" : android.text.format.DateFormat.getMediumDateFormat(getApplication()).format(timestamp)
                + " " + android.text.format.DateFormat.getTimeFormat(getApplication()).format(timestamp);
    }

    @Override protected void onCleared() { super.onCleared(); }
}
