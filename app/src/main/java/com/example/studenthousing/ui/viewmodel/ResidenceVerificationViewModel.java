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
        public final boolean isVerified;

        public ResidenceStatusData(String status, boolean isVerified) {
            this.status = status;
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

        ResidenceStatusData data = new ResidenceStatusData(st, isVerified);
        statusState.setValue(UiState.content(data, UiState.SyncState.SYNCED));
    }

    public void submitVerification(String userId, String selectedFilePath, String detailedAddress) {
        if (userId == null || userId.isEmpty()) {
            submitState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        if (selectedFilePath == null || selectedFilePath.isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng tải lên ảnh giấy xác nhận cư trú / CT07 / QR VNeID"));
            return;
        }
        if (detailedAddress == null || detailedAddress.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng nhập địa chỉ chi tiết"));
            return;
        }

        submitState.setValue(UiState.loading());

        String metadataJson = "{\"detailedAddress\":\"" + detailedAddress.trim().replace("\"", "\\\"") + "\"}";

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
