package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class SavedHousesViewModel extends AndroidViewModel {
    private final PostRepository repository;
    private final MutableLiveData<UiState<List<Post>>> savedPosts = new MutableLiveData<>(UiState.loading());

    public SavedHousesViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application);
    }

    public LiveData<UiState<List<Post>>> getSavedPosts() { return savedPosts; }

    public void unsavePost(String postId, AppCallback<Void> callback) {
        repository.unsavePost(postId, new AppCallback<Void>() {
            @Override public void onSuccess(Void value) {
                loadSavedPosts();
                if (callback != null) callback.onSuccess(null);
            }
            @Override public void onError(Exception error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void loadSavedPosts() {
        savedPosts.setValue(UiState.loading());
        repository.getSavedPosts(new AppCallback<List<Post>>() {
            @Override public void onSuccess(List<Post> posts) {
                savedPosts.postValue(UiState.content(posts == null ? new ArrayList<>() : posts, UiState.SyncState.SYNCED));
            }
            @Override public void onError(Exception e) {
                savedPosts.postValue(UiState.error(e == null ? "Lỗi tải danh sách đã lưu" : e.getMessage()));
            }
        });
    }
}
