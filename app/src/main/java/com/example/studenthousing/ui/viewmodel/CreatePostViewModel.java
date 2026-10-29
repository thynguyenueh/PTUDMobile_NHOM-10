package com.example.studenthousing.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.CloudinaryUploader;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.FileUtils;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CreatePostViewModel extends AndroidViewModel {

    private final PostRepository postRepository;
    private final SessionManager session;
    private final MutableLiveData<UiState<String>> submitState = new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        session = new SessionManager(application);
    }

    public void updatePost(String postId, List<Uri> selectedImageUris, String title, String address,
                           String desc,
                           List<String> utilities, int cityCode, int wardCode,
                           String wardName,
                           String houseNumber, String streetName) {
        if (postId == null || postId.trim().isEmpty()) {
            submitState.setValue(UiState.error("Không tìm thấy bài đăng cần cập nhật"));
            return;
        }
        submitState.setValue(UiState.loading());

        postRepository.getPostById(postId, new AppCallback<AccommodationPost>() {
            @Override
            public void onSuccess(AccommodationPost existing) {
                AccommodationPost post = existing != null ? existing : new AccommodationPost();
                post.setPostId(postId);
                post.setTitle(title);
                post.setAddress(address);
                post.setDescription(desc);
                post.setHouseNumber(houseNumber);
                post.setStreetName(streetName);

                post.setUtilities(utilities != null ? utilities : Collections.emptyList());
                post.setCityCode(cityCode > 0 ? cityCode : 79);
                post.setWardCode(wardCode > 0 ? wardCode : 27256);
                post.setCityName("TP. Hồ Chí Minh");
                if (wardName != null && !wardName.trim().isEmpty()) {
                    post.setWardName(wardName.trim());
                }
                post.setUpdatedAt(System.currentTimeMillis());

                Runnable persist = () -> postRepository.updatePost(post, new AppCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        submitState.postValue(UiState.content(postId, UiState.SyncState.SYNCED));
                    }

                    @Override
                    public void onError(Exception e) {
                        submitState.postValue(UiState.error("Cập nhật thất bại: "
                                + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
                    }
                });

                if (selectedImageUris == null || selectedImageUris.isEmpty()) {
                    persist.run();
                } else {
                    uploadImagesEdit(post, selectedImageUris, 0, new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                submitState.postValue(UiState.error("Không tìm thấy bài đăng: "
                        + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
            }
        });
    }

    private void uploadImagesEdit(AccommodationPost post, List<Uri> uris, int index, List<String> uploadedUrls) {
        if (index >= uris.size()) {
            post.setImages(new ArrayList<>(uploadedUrls));
            postRepository.updatePost(post, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    submitState.postValue(UiState.content(post.getPostId(), UiState.SyncState.SYNCED));
                }

                @Override
                public void onError(Exception e) {
                    submitState.postValue(UiState.error("Cập nhật thất bại: "
                            + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
                }
            });
            return;
        }

        Uri currentUri = uris.get(index);
        if (currentUri != null && ("http".equalsIgnoreCase(currentUri.getScheme()) || "https".equalsIgnoreCase(currentUri.getScheme()))) {
            uploadedUrls.add(currentUri.toString());
            uploadImagesEdit(post, uris, index + 1, uploadedUrls);
            return;
        }

        File imageFile = FileUtils.getFileFromUri(getApplication(), currentUri);
        if (imageFile == null) {
            submitState.postValue(UiState.error("Không thể đọc ảnh số " + (index + 1)));
            return;
        }
        CloudinaryUploader.uploadImage(imageFile, new AppCallback<String>() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedUrls.add(imageUrl);
                uploadImagesEdit(post, uris, index + 1, uploadedUrls);
            }

            @Override
            public void onError(Exception e) {
                String fallbackUrl = imageFile != null ? Uri.fromFile(imageFile).toString() : "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7";
                uploadedUrls.add(fallbackUrl);
                uploadImagesEdit(post, uris, index + 1, uploadedUrls);
            }
        });
    }

    public LiveData<UiState<String>> getSubmitState() {
        return submitState;
    }

    private void uploadImages(List<Uri> uris, int index, List<String> uploadedUrls,
                              String title, String address, String desc, List<String> utilities,
                              int cityCode, int wardCode, String wardName,
                              int addressVersion, String fingerprint, String houseNumber, String streetName) {

        Uri currentUri = uris.get(index);
        if (currentUri != null && ("http".equalsIgnoreCase(currentUri.getScheme()) || "https".equalsIgnoreCase(currentUri.getScheme()))) {
            uploadedUrls.add(currentUri.toString());
            uploadImages(uris, index + 1, uploadedUrls, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
            return;
        }

        File imageFile = FileUtils.getFileFromUri(getApplication(), currentUri);
        if (imageFile == null) {
            submitState.postValue(UiState.error("Không thể đọc ảnh số " + (index + 1)));
            return;
        }
        CloudinaryUploader.uploadImage(imageFile, new AppCallback<String>() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedUrls.add(imageUrl);
                uploadImages(uris, index + 1, uploadedUrls, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
            }

            @Override
            public void onError(Exception e) {
                String fallbackUrl = Uri.fromFile(imageFile).toString();
                uploadedUrls.add(fallbackUrl);
                uploadImages(uris, index + 1, uploadedUrls, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
            }
        });
    }
}
