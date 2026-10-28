package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.data.repository.ReviewRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailViewModel extends AndroidViewModel {

    public static class PostDetailData {
        public final AccommodationPost post;
        public final User host;
        public final List<Review> hostReviews;
        public final boolean isFavorite;

        public PostDetailData(AccommodationPost post, User host, List<Review> hostReviews, boolean isFavorite) {
            this.post = post;
            this.host = host;
            this.hostReviews = hostReviews;
            this.isFavorite = isFavorite;
        }
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    private final MutableLiveData<UiState<PostDetailData>> detailState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> favoriteState = new MutableLiveData<>();

    private AccommodationPost currentPost;
    private User currentHost;
    private List<Review> currentHostReviews;
    private boolean currentIsFavorite;

    public DetailViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
        reviewRepository = new ReviewRepository(application);
    }

    public LiveData<UiState<PostDetailData>> getDetailState() {
        return detailState;
    }

    public LiveData<Boolean> getFavoriteState() {
        return favoriteState;
    }

    public void loadPostDetail(String postId) {
        if (postId == null || postId.isEmpty()) {
            detailState.setValue(UiState.error("Không tìm thấy bài đăng"));
            return;
        }

        currentIsFavorite = postRepository.isSaved(postId);
        favoriteState.setValue(currentIsFavorite);

        executor.execute(() -> {
            AccommodationPost cachedPost = findCachedPost(postId);

            if (cachedPost != null) {
                currentPost = cachedPost;
                if (currentPost.getUserId() != null) {
                    currentHost = userRepository.getCachedUser(currentPost.getUserId());
                }
                PostDetailData localData = new PostDetailData(currentPost, currentHost, currentHostReviews, currentIsFavorite);
                detailState.postValue(UiState.content(localData, UiState.SyncState.PENDING_SYNC));
            } else {
                detailState.postValue(UiState.loading());
            }

            postRepository.getPostById(postId, new AppCallback<AccommodationPost>() {
                @Override
                public void onSuccess(AccommodationPost post) {
                    if (post != null) {
                        currentPost = post;
                        updateAndEmitDetail();
                        if (post.getUserId() != null) {
                            loadHostAndReviews(post.getUserId());
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (currentPost != null) {
                        PostDetailData data = new PostDetailData(currentPost, currentHost, currentHostReviews, currentIsFavorite);
                        detailState.postValue(UiState.content(data, UiState.SyncState.SYNC_ERROR));
                    } else {
                        detailState.postValue(UiState.error(e != null ? e.getMessage() : "Lỗi tải chi tiết bài đăng"));
                    }
                }
            });
        });
    }

    private AccommodationPost findCachedPost(String postId) {
        List<AccommodationPost> cachedPosts = postRepository.getCachedPosts();
        if (cachedPosts != null) {
            for (AccommodationPost p : cachedPosts) {
                if (postId.equals(p.getPostId())) {
                    return p;
                }
            }
        }
        return null;
    }

    private void loadHostAndReviews(String hostId) {
        userRepository.getUserProfile(hostId, new AppCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentHost = user;
                updateAndEmitDetail();
            }

            @Override
            public void onError(Exception e) {}
        });

        reviewRepository.getByReviewee(hostId, new AppCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                currentHostReviews = reviews;
                updateAndEmitDetail();
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void updateAndEmitDetail() {
        if (currentPost != null) {
            PostDetailData data = new PostDetailData(currentPost, currentHost, currentHostReviews, currentIsFavorite);
            detailState.postValue(UiState.content(data, UiState.SyncState.SYNCED));
        }
    }

    public void toggleFavorite(String postId) {
        if (postId == null || postId.isEmpty()) return;

        postRepository.toggleFavorite(postId, new AppCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean saved) {
                currentIsFavorite = Boolean.TRUE.equals(saved);
                favoriteState.postValue(currentIsFavorite);
                updateAndEmitDetail();
            }

            @Override
            public void onError(Exception e) {}
        });
    }
}
