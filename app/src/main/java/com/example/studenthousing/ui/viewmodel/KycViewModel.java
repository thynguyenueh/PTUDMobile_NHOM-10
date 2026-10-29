package com.example.studenthousing.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.data.repository.VerificationRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.CloudinaryUploader;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.FileUtils;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KycViewModel extends AndroidViewModel {

    private final SessionManager session;
    private final UserRepository userRepo;
    private final VerificationRepository verificationRepo;
    private final MutableLiveData<UiState<Boolean>> submitState = new MutableLiveData<>(UiState.content(false, UiState.SyncState.SYNCED));

    public KycViewModel(@NonNull Application application) {
        super(application);
        session = new SessionManager(application);
        userRepo = new UserRepository(application);
        verificationRepo = new VerificationRepository(application);
    }

    public LiveData<UiState<Boolean>> getSubmitState() {
        return submitState;
    }

    public void submitKyc(Uri parentFront, Uri parentBack, Uri studentFront, Uri studentBack, boolean isConsentChecked) {
        if (!isConsentChecked) {
            submitState.setValue(UiState.error("Vui lòng xác nhận đồng ý với quy trình xác minh"));
            return;
        }
        if (parentFront == null || parentBack == null) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ mặt trước và mặt sau căn cước công dân phụ huynh"));
            return;
        }
        if (studentFront == null || studentBack == null) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ mặt trước và mặt sau thẻ sinh viên"));
            return;
        }
        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            submitState.setValue(UiState.error("Không tìm thấy tài khoản. Vui lòng đăng nhập lại."));
            return;
        }
        submitState.setValue(UiState.loading());
        List<Uri> uris = new ArrayList<>();
        uris.add(parentFront);
        uris.add(parentBack);
        uris.add(studentFront);
        uris.add(studentBack);
        uploadKycDocuments(uris, 0, new ArrayList<>(), userId);
    }

    private void uploadKycDocuments(List<Uri> uris, int index, List<String> urls, String userId) {
        if (index >= uris.size()) {
            saveTicketAndUser(userId, urls);
            return;
        }
        Uri currentUri = uris.get(index);
        if (currentUri != null && ("http".equalsIgnoreCase(currentUri.getScheme()) || "https".equalsIgnoreCase(currentUri.getScheme()))) {
            urls.add(currentUri.toString());
            uploadKycDocuments(uris, index + 1, urls, userId);
            return;
        }
        File file = FileUtils.getFileFromUri(getApplication(), currentUri);
        if (file == null) {
            String fallbackUrl = currentUri != null ? currentUri.toString() : "";
            urls.add(fallbackUrl);
            uploadKycDocuments(uris, index + 1, urls, userId);
            return;
        }
        CloudinaryUploader.uploadImage(file, new AppCallback<String>() {
            @Override public void onSuccess(String url) {
                urls.add(url);
                uploadKycDocuments(uris, index + 1, urls, userId);
            }
            @Override public void onError(Exception e) {
                String fallbackUrl = Uri.fromFile(file).toString();
                urls.add(fallbackUrl);
                uploadKycDocuments(uris, index + 1, urls, userId);
            }
        });
    }

    private void saveTicketAndUser(String userId, List<String> urls) {
        String parentFrontUrl = urls.size() > 0 ? urls.get(0) : null;
        String parentBackUrl = urls.size() > 1 ? urls.get(1) : null;
        String studentFrontUrl = urls.size() > 2 ? urls.get(2) : null;
        String studentBackUrl = urls.size() > 3 ? urls.get(3) : null;
        String studentCardCombined = (studentFrontUrl != null ? studentFrontUrl : "") + "|" + (studentBackUrl != null ? studentBackUrl : "");

        String fileUrls = String.join("|", urls);
        verificationRepo.submitTicket(userId, Constants.TICKET_CITIZEN, fileUrls, null, new AppCallback<VerificationTicket>() {
            @Override
            public void onSuccess(VerificationTicket ticket) {
                userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, parentFrontUrl, parentBackUrl, studentCardCombined, new AppCallback<Void>() {
                    @Override public void onSuccess(Void d) {
                        submitState.postValue(UiState.content(true, UiState.SyncState.SYNCED));
                    }
                    @Override public void onError(Exception e) {
                        submitState.postValue(UiState.content(true, UiState.SyncState.SYNCED));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, parentFrontUrl, parentBackUrl, studentCardCombined, new AppCallback<Void>() {
                    @Override public void onSuccess(Void d) {
                        submitState.postValue(UiState.content(true, UiState.SyncState.SYNCED));
                    }
                    @Override public void onError(Exception ex) {
                        submitState.postValue(UiState.content(true, UiState.SyncState.SYNCED));
                    }
                });
            }
        });
    }
}
