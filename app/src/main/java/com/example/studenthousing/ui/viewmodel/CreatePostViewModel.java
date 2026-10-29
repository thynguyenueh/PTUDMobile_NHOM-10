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
import com.example.studenthousing.util.LocationService;
import com.example.studenthousing.util.NearbyCampusService;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreatePostViewModel extends AndroidViewModel {

    private final PostRepository postRepository;
    private final SessionManager session;
    private final MutableLiveData<UiState<String>> submitState =
            new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        session = new SessionManager(application);
    }

    public LiveData<UiState<String>> getSubmitState() {
        return submitState;
    }

    public void createPost(List<Uri> selectedImageUris,
                           String title,
                           String fullAddress,
                           String desc,
                           List<String> utilities,
                           int cityCode,
                           int wardCode,
                           String wardName,
                           String houseNumber,
                           String streetName) {

        if (title == null || title.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng nhập tiêu đề bài đăng"));
            return;
        }
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng nhập địa chỉ"));
            return;
        }
        if (selectedImageUris == null || selectedImageUris.isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng chọn ít nhất 1 hình ảnh"));
            return;
        }

        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng đăng nhập lại"));
            return;
        }

        submitState.setValue(UiState.loading());

        List<Uri> safeUris = new ArrayList<>(selectedImageUris);
        List<String> uploadedUrls = new ArrayList<>();
        uploadImagesCreate(safeUris, 0, uploadedUrls,
                title, fullAddress, desc, utilities,
                cityCode, wardCode, wardName, houseNumber, streetName);
    }

    private void uploadImagesCreate(List<Uri> uris,
                                    int index,
                                    List<String> uploadedUrls,
                                    String title,
                                    String fullAddress,
                                    String desc,
                                    List<String> utilities,
                                    int cityCode,
                                    int wardCode,
                                    String wardName,
                                    String houseNumber,
                                    String streetName) {

        if (index >= uris.size()) {
            persistNewPost(uploadedUrls, title, fullAddress, desc, utilities,
                    cityCode, wardCode, wardName, houseNumber, streetName);
            return;
        }

        Uri currentUri = uris.get(index);
        if (currentUri == null) {
            uploadImagesCreate(uris, index + 1, uploadedUrls,
                    title, fullAddress, desc, utilities,
                    cityCode, wardCode, wardName, houseNumber, streetName);
            return;
        }

        String scheme = currentUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            uploadedUrls.add(currentUri.toString());
            uploadImagesCreate(uris, index + 1, uploadedUrls,
                    title, fullAddress, desc, utilities,
                    cityCode, wardCode, wardName, houseNumber, streetName);
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
                uploadImagesCreate(uris, index + 1, uploadedUrls,
                        title, fullAddress, desc, utilities,
                        cityCode, wardCode, wardName, houseNumber, streetName);
            }

            @Override
            public void onError(Exception e) {
                String fallbackUrl = Uri.fromFile(imageFile).toString();
                uploadedUrls.add(fallbackUrl);
                uploadImagesCreate(uris, index + 1, uploadedUrls,
                        title, fullAddress, desc, utilities,
                        cityCode, wardCode, wardName, houseNumber, streetName);
            }
        });
    }

    private void persistNewPost(List<String> uploadedUrls,
                                String title,
                                String fullAddress,
                                String desc,
                                List<String> utilities,
                                int cityCode,
                                int wardCode,
                                String wardName,
                                String houseNumber,
                                String streetName) {

        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            submitState.postValue(UiState.error("Vui lòng đăng nhập lại"));
            return;
        }

        long now = System.currentTimeMillis();
        AccommodationPost post = new AccommodationPost();
        post.setPostId("post_" + now);
        post.setUserId(userId);
        post.setTitle(title);
        post.setAddress(fullAddress);
        post.setDescription(desc);
        post.setImages(uploadedUrls);
        post.setUtilities(utilities != null ? utilities : Collections.emptyList());
        post.setCityCode(cityCode > 0 ? cityCode : 79);
        post.setWardCode(wardCode > 0 ? wardCode : 27256);
        post.setCityName(Constants.TPHCM_CITY_NAME);
        if (wardName != null && !wardName.trim().isEmpty()) {
            post.setWardName(wardName.trim());
        }
        post.setHouseNumber(houseNumber);
        post.setStreetName(streetName);
        post.setFamilyName(session.getUserFamilyName());
        post.setStatus(Constants.POST_AVAILABLE);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);

        LocationService.getCoordinatesFromAddress(
                getApplication(), fullAddress, wardName,
                new AppCallback<android.location.Location>() {
                    @Override
                    public void onSuccess(android.location.Location loc) {
                        post.setLatitude(loc.getLatitude());
                        post.setLongitude(loc.getLongitude());
                        post.setAddressValidationStatus(Constants.ADDR_VALID);

                        // Tính khoảng cách bằng OSRM Table định tuyến thực tế
                        NearbyCampusService.calculateNearbyWithRouting(
                                getApplication(), loc.getLatitude(), loc.getLongitude(),
                                campuses -> {
                                    post.setNearbyCampuses(campuses != null ? campuses : Collections.emptyList());
                                    savePostToRepo(post);
                                });
                    }

                    @Override
                    public void onError(Exception e) {
                        submitState.postValue(UiState.error("Không thể xác định vị trí thực tế của địa chỉ này để tính khoảng cách tới trường. Vui lòng thử lại."));
                    }
                });
    }

    private void savePostToRepo(AccommodationPost post) {
        postRepository.createPost(post, new AppCallback<String>() {
            @Override
            public void onSuccess(String postId) {
                submitState.postValue(UiState.content(postId, UiState.SyncState.SYNCED));
            }

            @Override
            public void onError(Exception e) {
                submitState.postValue(UiState.error("Đăng bài thất bại: "
                        + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
            }
        });
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
                post.setCityName(Constants.TPHCM_CITY_NAME);
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
}