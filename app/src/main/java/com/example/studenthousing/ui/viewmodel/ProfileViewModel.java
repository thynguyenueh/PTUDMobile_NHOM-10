package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.UiState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {

    public static class ProfileData {
        public final User user;
        public final int postCount;
        public final int exchangeCount;
        public final int reviewCount;
        public final String residenceStatus;
        public final String kycStatus;

        public ProfileData(User user, int postCount, int exchangeCount, int reviewCount,
                           String residenceStatus, String kycStatus) {
            this.user = user;
            this.postCount = postCount;
            this.exchangeCount = exchangeCount;
            this.reviewCount = reviewCount;
            this.residenceStatus = residenceStatus;
            this.kycStatus = kycStatus;
        }
    }

    private final UserRepository userRepository;
    private final UserDao userDao;
    private final PostDao postDao;
    private final MutableLiveData<UiState<ProfileData>> profileState = new MutableLiveData<>(UiState.loading());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        userDao = new UserDao(application);
        postDao = new PostDao(application);
    }

    public LiveData<UiState<ProfileData>> getProfileState() {
        return profileState;
    }

    public void loadProfile(String userId) {
        if (userId == null || userId.isEmpty()) {
            profileState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        executor.execute(() -> {
            ProfileData localData = buildProfileData(userId);
            if (localData != null) {
                profileState.postValue(UiState.content(localData, UiState.SyncState.PENDING_SYNC));
            } else {
                profileState.postValue(UiState.loading());
            }

            userRepository.getUserProfile(userId, new AppCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    executor.execute(() -> {
                        ProfileData updated = buildProfileData(userId);
                        profileState.postValue(UiState.content(updated, UiState.SyncState.SYNCED));
                    });
                }

                @Override
                public void onError(Exception e) {
                    executor.execute(() -> {
                        ProfileData fallback = buildProfileData(userId);
                        if (fallback != null && fallback.user != null) {
                            profileState.postValue(UiState.content(fallback, UiState.SyncState.SYNC_ERROR));
                        } else {
                            profileState.postValue(UiState.error(e != null && e.getMessage() != null ? e.getMessage() : "Lỗi tải hồ sơ"));
                        }
                    });
                }
            });
        });
    }

    private ProfileData buildProfileData(String userId) {
        User user = userDao.getById(userId);
        int postCount = postDao.countByUserId(userId);
        int exchangeCount = 0;
        int reviewCount = 0;

        String residenceStatus = (user != null && user.isResidenceVerified()) ? Constants.VERIF_APPROVED : Constants.VERIF_NONE;
        String kycStatus = (user != null && user.isIdentityVerified()) ? Constants.VERIF_APPROVED : Constants.VERIF_NONE;

        return new ProfileData(user, postCount, exchangeCount, reviewCount, residenceStatus, kycStatus);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
