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

    public void checkFirestoreDuplicate(String fingerprint, AppCallback<Boolean> cb) {
        if (fingerprint == null || fingerprint.isEmpty() || !NetworkUtils.isOnline(getApplication())) {
            if (cb != null) cb.onSuccess(false);
            return;
        }
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("accommodation_posts")
                .whereEqualTo("addressFingerprint", fingerprint)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean exists = snap != null && !snap.isEmpty();
                    if (cb != null) cb.onSuccess(exists);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onSuccess(false);
                });
    }

    public void updatePost(String postId, List<Uri> selectedImageUris, String title, String address, String desc,
                           List<String> utilities, int cityCode, int wardCode) {
        updatePost(postId, selectedImageUris, title, address, desc, utilities, cityCode, wardCode, "", 2, "", "", "");
    }

    public void updatePost(String postId, List<Uri> selectedImageUris, String title, String address,
                           String desc,
                           List<String> utilities, int cityCode, int wardCode,
                           String wardName, int addressVersion, String fingerprint) {
        updatePost(postId, selectedImageUris, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, "", "");
    }

    public void updatePost(String postId, List<Uri> selectedImageUris, String title, String address,
                           String desc,
                           List<String> utilities, int cityCode, int wardCode,
                           String wardName, int addressVersion, String fingerprint,
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
                if (fingerprint != null && !fingerprint.isEmpty()) post.setAddressFingerprint(fingerprint);
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

        File imageFile = FileUtils.getFileFromUri(getApplication(), uris.get(index));
        if (imageFile == null) {
            submitState.postValue(UiState.error("Không thể đọc ảnh số " + (index + 1)));
            return;
        }
        CloudinaryUploader.uploadImage(imageFile, new CloudinaryUploader.UploadCallback() {
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

    public void createPost(List<Uri> selectedImageUris, String title, String address, String desc,
                           List<String> utilities, int cityCode, int wardCode, String wardName,
                           String houseNumber, String streetName) {
        submitPost(selectedImageUris, title, address, desc, utilities, cityCode, wardCode, wardName, 2, "", houseNumber, streetName);
    }

    public void submitPost(List<Uri> selectedImageUris, String title, String address, String desc, List<String> utilities) {
        submitPost(selectedImageUris, title, address, desc, utilities, 79, 27256, "", 2, "", "", "");
    }

    public void submitPost(List<Uri> selectedImageUris, String title, String address, String desc,
                           List<String> utilities, int cityCode, int wardCode) {
        submitPost(selectedImageUris, title, address, desc, utilities, cityCode, wardCode, "", 2, "", "", "");
    }

    public void submitPost(List<Uri> selectedImageUris, String title, String address, String desc,
                           List<String> utilities, int cityCode, int wardCode, String wardName,
                           int addressVersion, String fingerprint) {
        submitPost(selectedImageUris, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, "", "");
    }

    public void submitPost(List<Uri> selectedImageUris, String title, String address, String desc,
                           List<String> utilities, int cityCode, int wardCode, String wardName,
                           int addressVersion, String fingerprint, String houseNumber, String streetName) {
        if (selectedImageUris == null || selectedImageUris.isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng chọn ít nhất 1 ảnh"));
            return;
        }

        submitState.setValue(UiState.loading());
        if (NetworkUtils.isOnline(getApplication())) {
            uploadImages(selectedImageUris, 0, new ArrayList<>(), title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
        } else {
            List<String> localUris = new ArrayList<>();
            for (Uri uri : selectedImageUris) {
                if (uri != null) {
                    String localFilePath = FileUtils.saveUriToLocalFile(getApplication(), uri);
                    if (localFilePath != null) localUris.add(localFilePath);
                }
            }
            calculateDistanceAndSave(localUris, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
        }
    }

    private void uploadImages(List<Uri> uris, int index, List<String> uploadedUrls,
                              String title, String address, String desc, List<String> utilities,
                              int cityCode, int wardCode, String wardName,
                              int addressVersion, String fingerprint, String houseNumber, String streetName) {
        if (index >= uris.size()) {
            calculateDistanceAndSave(uploadedUrls, title, address, desc, utilities, cityCode, wardCode, wardName, addressVersion, fingerprint, houseNumber, streetName);
            return;
        }

        File imageFile = FileUtils.getFileFromUri(getApplication(), uris.get(index));
        if (imageFile == null) {
            submitState.postValue(UiState.error("Không thể đọc ảnh số " + (index + 1)));
            return;
        }
        CloudinaryUploader.uploadImage(imageFile, new CloudinaryUploader.UploadCallback() {
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

    private void calculateDistanceAndSave(List<String> uploadedImageUrls, String title, String address,
                                         String desc, List<String> utilities,
                                         int cityCode, int wardCode, String wardName,
                                         int addressVersion, String fingerprint, String houseNumber, String streetName) {
        String addrLower = address != null ? address.toLowerCase() : "";
        double houseLat;
        double houseLon;
        if (addrLower.contains("thủ đức") || addrLower.contains("linh xuân") || addrLower.contains("linh trung") || addrLower.contains("hiệp phú")) {
            houseLat = 10.8655; houseLon = 106.7612;
        } else if (addrLower.contains("thảo điền") || addrLower.contains("quận 2") || addrLower.contains("an phú") || addrLower.contains("thủ thiêm")) {
            houseLat = 10.8016; houseLon = 106.7404;
        } else if (addrLower.contains("bình thạnh") || addrLower.contains("đinh bộ lĩnh") || addrLower.contains("xô viết nghệ tĩnh")) {
            houseLat = 10.8038; houseLon = 106.7032;
        } else if (addrLower.contains("tân phú") || addrLower.contains("tân bình") || addrLower.contains("gò vấp")) {
            houseLat = 10.7900; houseLon = 106.6400;
        } else if (addrLower.contains("quận 7") || addrLower.contains("tân phong") || addrLower.contains("phú mỹ hưng")) {
            houseLat = 10.7340; houseLon = 106.7210;
        } else {
            houseLat = 10.7769; houseLon = 106.7009;
        }

        final double finalHouseLat = houseLat;
        final double finalHouseLon = houseLon;

        String currentUserId = SessionManager.getUid(getApplication());

        savePostWithDistance(uploadedImageUrls, title, address, desc,
                utilities, cityCode, wardCode, wardName, addressVersion, fingerprint,
                currentUserId, finalHouseLat, finalHouseLon, houseNumber, streetName);
    }

    private void savePostWithDistance(List<String> uploadedImageUrls, String title, String address, String desc,
                                      List<String> utilities,
                                      int cityCode, int wardCode, String wardName,
                                      int addressVersion, String fingerprint,
                                      String currentUserId, double finalHouseLat, double finalHouseLon,
                                      String houseNumber, String streetName) {
        String familyName = session.getUserFamilyName() != null ? session.getUserFamilyName() : "Hộ gia đình sinh viên";

        long now = System.currentTimeMillis();
        AccommodationPost post = new AccommodationPost();
        post.setPostId("temp_" + UUID.randomUUID().toString());
        post.setTitle(title);
        post.setAddress(address);
        post.setDescription(desc);
        post.setHouseNumber(houseNumber);
        post.setStreetName(streetName);
        post.setUserId(currentUserId);
        post.setFamilyName(familyName);
        post.setStatus(Constants.POST_AVAILABLE);
        post.setUtilities(utilities != null ? utilities : Collections.emptyList());
        post.setCityCode(cityCode > 0 ? cityCode : 79);
        post.setWardCode(wardCode > 0 ? wardCode : 27256);
        post.setCityName("TP. Hồ Chí Minh");
        if (wardName != null && !wardName.trim().isEmpty()) {
            post.setWardName(wardName.trim());
        }
        // addressVersion is intentionally ignored in the v2 ward-only schema.
        post.setAddressFingerprint(fingerprint);
        post.setImages(new ArrayList<>(uploadedImageUrls));
        post.setCreatedAt(now);
        post.setUpdatedAt(now);

        postRepository.createPost(post, new AppCallback<String>() {
            @Override
            public void onSuccess(String postId) {
                UiState.SyncState syncState = NetworkUtils.isOnline(getApplication())
                        ? UiState.SyncState.SYNCED
                        : UiState.SyncState.PENDING_SYNC;
                submitState.postValue(UiState.content(postId, syncState));
            }

            @Override
            public void onError(Exception e) {
                submitState.postValue(UiState.error("Lỗi lưu bài đăng: " + (e != null ? e.getMessage() : "")));
            }
        });
    }
}
