package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListViewModel extends AndroidViewModel {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ChatRepository repository;
    private final MutableLiveData<UiState<List<ChatRoom>>> rooms = new MutableLiveData<>(UiState.loading());
    private ListenerRegistration listener;

    public ChatListViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    public LiveData<UiState<List<ChatRoom>>> getRooms() { return rooms; }

    public void observeRooms(String userId) {
        if (listener != null) listener.remove();
        if (userId == null || userId.isEmpty()) {
            rooms.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }
        rooms.setValue(UiState.loading());

        executor.execute(() -> {
            List<ChatRoom> cached = repository.getUserRoomsCached(userId);
            if (cached != null && !cached.isEmpty()) {
                rooms.postValue(UiState.content(cached, UiState.SyncState.PENDING_SYNC));
            }

            listener = repository.listenUserRooms(userId, new AppCallback<List<ChatRoom>>() {
                @Override public void onSuccess(List<ChatRoom> data) {
                    rooms.postValue(UiState.content(data == null ? new ArrayList<>() : data, UiState.SyncState.SYNCED));
                }
                @Override public void onError(Exception e) {
                    List<ChatRoom> fallback = repository.getUserRoomsCached(userId);
                    if (fallback != null && !fallback.isEmpty()) {
                        rooms.postValue(UiState.content(fallback, UiState.SyncState.SYNC_ERROR));
                    } else {
                        rooms.postValue(UiState.error(e == null ? "Lỗi tải hội thoại" : e.getMessage()));
                    }
                }
            });
        });
    }

    @Override
    protected void onCleared() {
        if (listener != null) listener.remove();
        super.onCleared();
    }
}
