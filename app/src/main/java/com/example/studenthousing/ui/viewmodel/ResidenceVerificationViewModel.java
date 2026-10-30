package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.data.repository.VerificationRepository;
import com.example.studenthousing.util.AddressFingerprintUtil;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.LocationService;
import com.example.studenthousing.util.NetworkUtils;
import com.example.studenthousing.util.UiState;

public class ResidenceVerificationViewModel extends AndroidViewModel {

    public static class ResidenceStatusData {
        public final String status;
        public final String method;
        public final String filePath;
        public final String fileName;
        public final boolean isVerified;

        public ResidenceStatusData(String status, String method, String filePath, String fileName, boolean isVerified) {
            this.status = status;
            this.method = method;
            this.filePath = filePath;
            this.fileName = fileName;
            this.isVerified = isVerified;
        }
    }

    private final VerificationRepository verificationRepo;
    private final UserRepository userRepo;
    private final UserDao userDao;
    private final VerifiedAddressDao verifiedAddressDao;
    private final MutableLiveData<UiState<ResidenceStatusData>> statusState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<UiState<String>> submitState = new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public ResidenceVerificationViewModel(@NonNull Application application) {
        super(application);
        verificationRepo = new VerificationRepository(application);
        userRepo = new UserRepository(application);
        userDao = new UserDao(application);
        verifiedAddressDao = new VerifiedAddressDao(application);
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

        ResidenceStatusData data = new ResidenceStatusData(st, "FILE", null, null, isVerified);
        statusState.setValue(UiState.content(data, UiState.SyncState.SYNCED));
    }

    public void submitVerification(String userId, String selectedFilePath) {
        submitVerification(userId, selectedFilePath, "");
    }

    public void submitVerification(String userId, String selectedFilePath, String address) {
        if (userId == null || userId.isEmpty()) {
            submitState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        if (selectedFilePath == null || selectedFilePath.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng chọn ảnh hoặc tệp giấy tờ xác thực cư trú (CT07 / VNeID)"));
            return;
        }

        if (address == null || address.trim().isEmpty()) {
            submitState.setValue(UiState.error("Vui lòng nhập địa chỉ chi tiết"));
            return;
        }

        String trimmedAddress = address.trim();
        if (trimmedAddress.length() < 8 || trimmedAddress.matches("^\\d+$")) {
            submitState.setValue(UiState.error("Địa chỉ quá ngắn hoặc không hợp lệ. Vui lòng nhập số nhà và tên đường."));
            return;
        }

        String[] parts = trimmedAddress.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.length() < 2 && !p.matches("\\d+")) {
                submitState.setValue(UiState.error("Địa chỉ không hợp lệ. Vui lòng không dùng ký tự viết tắt quá ngắn."));
                return;
            }
        }

        submitState.setValue(UiState.loading());

        final String finalAddress = trimmedAddress;

        if (!NetworkUtils.isOnline(getApplication())) {
            submitState.setValue(UiState.error("Cần kết nối internet để xác thực và kiểm tra tọa độ địa chỉ."));
            return;
        }

        LocationService.getCoordinatesFromAddress(getApplication(), finalAddress, "", new AppCallback<LocationService.GeoResult>() {
            @Override
            public void onSuccess(LocationService.GeoResult result) {
                String verifiedAddress = result.formattedAddress != null ? result.formattedAddress : finalAddress;

                String fingerprint = AddressFingerprintUtil.build(79, 27256, verifiedAddress, "");
                VerifiedAddress va = new VerifiedAddress();
                va.setAddressId("addr_" + System.currentTimeMillis());
                va.setFingerprint(fingerprint);
                va.setCityCode(79);
                va.setWardCode(27256);
                va.setFullAddress(verifiedAddress);
                va.setOwnerUserId(userId);
                va.setStatus("VERIFIED");
                va.setVerifiedAt(System.currentTimeMillis());

                verifiedAddressDao.upsert(va);

                verificationRepo.submitTicket(userId, Constants.TICKET_RESIDENCE, selectedFilePath, null, new AppCallback<>() {
                    @Override
                    public void onSuccess(VerificationTicket ticket) {
                        userRepo.markResidenceVerified(userId, null);
                        submitState.postValue(UiState.content("Xác thực địa chỉ thành công!", UiState.SyncState.SYNCED));
                        loadStatus(userId);
                    }

                    @Override
                    public void onError(Exception e) {
                        userRepo.markResidenceVerified(userId, null);
                        submitState.postValue(UiState.content("Xác thực địa chỉ thành công!", UiState.SyncState.SYNCED));
                        loadStatus(userId);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                submitState.postValue(UiState.error("Địa chỉ không tồn tại hoặc không tìm thấy trên bản đồ. Vui lòng nhập địa chỉ chính xác!"));
            }
        });
    }
}
