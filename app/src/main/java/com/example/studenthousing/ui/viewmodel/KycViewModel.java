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

    public void submitKyc(Uri parentFront, Uri parentBack, Uri studentId, boolean isConsentChecked) {
        if (!isConsentChecked) {
            submitState.setValue(UiState.error("Vui lòng xác nhận đồng ý với quy trình xác minh"));
            return;
        }
        if (parentFront == null || parentBack == null) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ mặt trước và sau CCCD phụ huynh"));
            return;
        }
        if (studentId == null) {
            submitState.setValue(UiState.error("Vui lòng tải lên Thẻ sinh viên"));
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
        uris.add(studentId);
        uploadKycDocuments(uris, 0, new ArrayList<>(), userId);
    }

    private void uploadKycDocuments(List<Uri> uris, int index, List<String> urls, String userId) {
        if (index >= uris.size()) {
            saveTicket(userId, urls);
            return;
        }
        File file = FileUtils.getFileFromUri(getApplication(), uris.get(index));
        if (file == null) {
            submitState.postValue(UiState.error("Không thể đọc giấy tờ số " + (index + 1)));
            return;
        }
        CloudinaryUploader.uploadImage(file, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) {
                urls.add(url);
                uploadKycDocuments(uris, index + 1, urls, userId);
            }
            @Override public void onError(Exception e) {
                submitState.postValue(UiState.error("Tải giấy tờ thất bại: " + (e != null ? e.getMessage() : "Lỗi không xác định")));
            }
        });
    }

    private void saveTicket(String userId, List<String> urls) {
        String fileUrls = String.join("|", urls);
        verificationRepo.submitTicket(userId, Constants.TICKET_STUDENT, fileUrls, null, new AppCallback<>() {
            @Override
            public void onSuccess(VerificationTicket ticket) {
                userRepo.markIdentityVerified(userId, Constants.IDENTITY_STUDENT, null);
                submitState.postValue(UiState.content(true, UiState.SyncState.SYNCED));
            }

            @Override
            public void onError(Exception e) {
                submitState.postValue(UiState.error("Gửi hồ sơ xác minh thất bại: "
                        + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
            }
        });
    }
}
