package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.data.repository.VerificationRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.UiState;

public class ResidenceVerificationViewModel extends AndroidViewModel {

    public static class ResidenceStatusData {
        public final String status;
        public final String method;
        public final String filePath;
        public final String fileName;
        public final String fileUrl;
        public final String regNumber;
        public final String ward;
        public final boolean isVerified;

        public ResidenceStatusData(String status, String method, String filePath, String fileName,
                                   String fileUrl, String regNumber, String ward, boolean isVerified) {
            this.status = status;
            this.method = method;
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.regNumber = regNumber;
            this.ward = ward;
            this.isVerified = isVerified;
        }
    }

    private final VerificationRepository verificationRepo;
    private final UserRepository userRepo;
    private final UserDao userDao;
    private final MutableLiveData<UiState<ResidenceStatusData>> statusState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<UiState<String>> submitState = new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public ResidenceVerificationViewModel(@NonNull Application application) {
        super(application);
        verificationRepo = new VerificationRepository(application);
        userRepo = new UserRepository(application);
        userDao = new UserDao(application);
    }

    public LiveData<UiState<ResidenceStatusData>> getStatusState() { return statusState; }
    public LiveData<UiState<String>> getSubmitState() { return submitState; }

    public void loadStatus(String userId) {
        if (userId == null || userId.isEmpty()) {
            statusState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        User u = userDao.getById(userId);
        boolean isVerified = u != null && u.isResidenceVerified();
        String st = isVerified ? Constants.VERIF_APPROVED : Constants.VERIF_NONE;

        ResidenceStatusData data = new ResidenceStatusData(st, "FILE", null, null, null, null, null, isVerified);
        statusState.setValue(UiState.content(data, UiState.SyncState.SYNCED));
    }

    public void submitVerification(String userId, boolean isCt07Option, String selectedFilePath,
                                   String selectedFileName, String regNumber, String selectedWard) {
        if (userId == null || userId.isEmpty()) {
            submitState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        if (isCt07Option) {
            if (selectedFilePath == null) {
                submitState.setValue(UiState.error("Vui lòng chọn tệp CT07 / VNeID dạng PDF hoặc ảnh"));
                return;
            }
        } else {
            if (regNumber == null || regNumber.trim().length() < 6) {
                submitState.setValue(UiState.error("Nhập mã hồ sơ Dịch vụ công (ít nhất 6 ký tự)"));
                return;
            }
            if (selectedWard == null) {
                submitState.setValue(UiState.error("Vui lòng chọn Phường/Xã tạm trú"));
                return;
            }
        }

        submitState.setValue(UiState.loading());

        String metadataJson = "{\"regNumber\":\"" + (regNumber != null ? regNumber.trim() : "") + "\",\"ward\":\"" + (selectedWard != null ? selectedWard : "") + "\"}";

        verificationRepo.submitTicket(userId, Constants.TICKET_RESIDENCE, selectedFilePath, metadataJson, new AppCallback<>() {
            @Override
            public void onSuccess(VerificationTicket ticket) {
                userRepo.markResidenceVerified(userId, null);
                submitState.setValue(UiState.content("Hồ sơ đã được gửi đối soát thành công!", UiState.SyncState.SYNCED));
                loadStatus(userId);
            }

            @Override
            public void onError(Exception e) {
                submitState.setValue(UiState.error("Gửi hồ sơ tạm trú thất bại: "
                        + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
            }
        });
    }
}
