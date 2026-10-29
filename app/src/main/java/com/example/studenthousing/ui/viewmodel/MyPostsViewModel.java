package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyPostsViewModel extends AndroidViewModel {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PostDao postDao;
    private final PostRepository repository;
    private final MutableLiveData<UiState<List<AccommodationPost>>> state = new MutableLiveData<>(UiState.loading());

    public MyPostsViewModel(@NonNull Application application) {
        super(application);
        postDao = new PostDao(application);
        repository = new PostRepository(application);
    }

    public LiveData<UiState<List<AccommodationPost>>> getState() { return state; }

    public void load(String userId) {
        if (userId == null || userId.trim().isEmpty() || "current_user".equals(userId)) {
            state.postValue(UiState.error("Chưa đăng nhập"));
            return;
        }
        state.setValue(UiState.loading());
        executor.execute(() -> {
            List<AccommodationPost> cached = postDao.getByUserId(userId);
            if (cached != null && !cached.isEmpty()) {
                state.postValue(UiState.content(cached, UiState.SyncState.PENDING_SYNC));
            }

            repository.getPostsByUserId(userId, new AppCallback<List<AccommodationPost>>() {
                @Override public void onSuccess(List<AccommodationPost> posts) {
                    List<AccommodationPost> result = posts != null ? posts : cached;
                    state.postValue(UiState.content(result == null ? new ArrayList<>() : result, UiState.SyncState.SYNCED));
                }
                @Override public void onError(Exception e) {
                    List<AccommodationPost> fallback = postDao.getByUserId(userId);
                    state.postValue(UiState.content(fallback != null ? fallback : new ArrayList<>(), UiState.SyncState.SYNC_ERROR));
                }
            });
        });
    }

    public void delete(String postId, AppCallback<Void> callback) { repository.deletePost(postId, callback); }
}
