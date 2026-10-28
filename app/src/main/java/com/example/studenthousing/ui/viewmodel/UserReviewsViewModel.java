package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.List;

public class UserReviewsViewModel extends AndroidViewModel {
    private final ReviewRepository repository;
    private final MutableLiveData<UiState<List<Review>>> reviews = new MutableLiveData<>(UiState.loading());
    private String loadedUserId;

    public UserReviewsViewModel(@NonNull Application application) {
        super(application);
        repository = new ReviewRepository(application);
    }

    public LiveData<UiState<List<Review>>> getReviews() { return reviews; }

    public void load(String userId, boolean force) {
        if (userId == null || userId.isEmpty()) {
            reviews.setValue(UiState.error("Không tìm thấy người dùng"));
            return;
        }
        if (!force && userId.equals(loadedUserId)) return;
        loadedUserId = userId;
        reviews.setValue(UiState.loading());
        repository.getByReviewee(userId, new AppCallback<List<Review>>() {
            @Override public void onSuccess(List<Review> data) {
                reviews.postValue(UiState.content(data == null ? new ArrayList<>() : data, UiState.SyncState.SYNCED));
            }
            @Override public void onError(Exception e) {
                reviews.postValue(UiState.error(e == null ? "Lỗi tải đánh giá" : e.getMessage()));
            }
        });
    }
}
