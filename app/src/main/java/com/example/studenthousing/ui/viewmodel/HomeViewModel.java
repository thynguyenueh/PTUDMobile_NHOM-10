package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.Post;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public final class HomeViewModel extends AndroidViewModel {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<UiState<List<Post>>> posts = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<UiState<User>> profile = new MutableLiveData<>(UiState.loading());
    private boolean postsRequested;
    private String loadedProfileUid;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<UiState<List<Post>>> getPosts() { return posts; }
    public LiveData<UiState<User>> getProfile() { return profile; }

    public void loadPosts() {
        if (postsRequested) return;
        postsRequested = true;

        List<Post> cached = postRepository.getCachedPosts();
        if (cached != null && !cached.isEmpty()) {
            posts.setValue(UiState.content(cached, UiState.SyncState.PENDING_SYNC));
        } else {
            posts.setValue(UiState.loading());
        }

        postRepository.getLatestPosts(50, new AppCallback<List<Post>>() {
            @Override public void onSuccess(List<Post> data) {
                List<Post> safe = data == null ? new ArrayList<>() : data;
                posts.postValue(UiState.content(safe, UiState.SyncState.SYNCED));
            }
            @Override public void onError(Exception e) {
                postsRequested = false;
                List<Post> fallback = postRepository.getCachedPosts();
                if (fallback != null && !fallback.isEmpty()) {
                    posts.postValue(UiState.content(fallback, UiState.SyncState.SYNC_ERROR));
                } else {
                    posts.postValue(UiState.error(message(e)));
                }
            }
        });
    }

    public void refreshPosts() {
        postsRequested = false;
        loadPosts();
    }

    public void loadProfile(String userId) {
        if (userId == null || userId.isEmpty()) {
            profile.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }
        if (userId.equals(loadedProfileUid)) return;
        loadedProfileUid = userId;

        User cachedUser = userRepository.getCachedUser(userId);
        if (cachedUser != null) {
            profile.setValue(UiState.content(cachedUser, UiState.SyncState.PENDING_SYNC));
        } else {
            profile.setValue(UiState.loading());
        }

        userRepository.getUserProfile(userId, new AppCallback<User>() {
            @Override public void onSuccess(User user) {
                profile.postValue(UiState.content(user, UiState.SyncState.SYNCED));
            }
            @Override public void onError(Exception e) {
                User fallback = userRepository.getCachedUser(userId);
                if (fallback != null) {
                    profile.postValue(UiState.content(fallback, UiState.SyncState.SYNC_ERROR));
                } else {
                    profile.postValue(UiState.error(message(e)));
                }
            }
        });
    }

    public void refreshProfile(String userId) {
        loadedProfileUid = null;
        loadProfile(userId);
    }

    private static String message(Exception e) {
        return e == null || e.getLocalizedMessage() == null ? "Đã xảy ra lỗi" : e.getLocalizedMessage();
    }
}
