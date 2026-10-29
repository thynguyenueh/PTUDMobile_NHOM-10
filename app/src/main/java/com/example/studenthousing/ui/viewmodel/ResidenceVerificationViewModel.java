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
        public final java.util.List<com.example.studenthousing.data.model.VerifiedAddress> verifiedAddresses;

        public ResidenceStatusData(String status, boolean isVerified,
                                   java.util.List<com.example.studenthousing.data.model.VerifiedAddress> verifiedAddresses) {
            this.status = status;
            this.isVerified = isVerified;
            this.verifiedAddresses = verifiedAddresses;
        }
    }

    private final VerificationRepository verificationRepo;
    private final UserRepository userRepo;
    private final UserDao userDao;
    private final com.example.studenthousing.data.local.VerifiedAddressDao verifiedAddressDao;
    private final MutableLiveData<UiState<ResidenceStatusData>> statusState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<UiState<String>> submitState = new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public ResidenceVerificationViewModel(@NonNull Application application) {
        super(application);
        verificationRepo = new VerificationRepository(application);
        userRepo = new UserRepository(application);
        userDao = new UserDao(application);
        verifiedAddressDao = new com.example.studenthousing.data.local.VerifiedAddressDao(application);
    }

    public LiveData<UiState<ResidenceStatusData>> getStatusState() { return statusState; }
    public LiveData<UiState<String>> getSubmitState() { return submitState; }

    public void loadStatus(String userId) {
        if (userId == null || userId.isEmpty()) {
            statusState.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        User u = userDao.getById(userId);
        java.util.List<com.example.studenthousing.data.model.VerifiedAddress> list = verifiedAddressDao.getByUserId(userId);
        boolean isVerified = (u != null && u.isResidenceVerified()) || (list != null && !list.isEmpty());
        String st = isVerified ? Constants.VERIF_APPROVED : Constants.VERIF_NONE;

        ResidenceStatusData data = new ResidenceStatusData(st, isVerified, list);
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

        final String cleanAddress = detailedAddress.trim();

        // Bước 1: Geocode kiểm tra địa chỉ có tồn tại và xác định được tọa độ không
        com.example.studenthousing.util.LocationService.getCoordinatesFromAddress(
                getApplication(), cleanAddress, "", new AppCallback<>() {
                    @Override
                    public void onSuccess(android.location.Location location) {
                        // Bước 2: Tạo Ticket & Lưu VerifiedAddress
                        String metadataJson = "{\"detailedAddress\":\"" + cleanAddress.replace("\"", "\\\"") + "\"}";
                        verificationRepo.submitTicket(userId, Constants.TICKET_RESIDENCE, selectedFilePath, metadataJson, new AppCallback<>() {
                            @Override
                            public void onSuccess(VerificationTicket ticket) {
                                String addressId = "addr_" + System.currentTimeMillis();
                                com.example.studenthousing.data.model.VerifiedAddress va =
                                        new com.example.studenthousing.data.model.VerifiedAddress(
                                                addressId, cleanAddress, userId, 79, 0, ticket.getTicketId());
                                verifiedAddressDao.upsert(va);

                                userRepo.markResidenceVerified(userId, null);
                                submitState.setValue(UiState.content("Xác thực địa chỉ thành công và đã thêm vào danh sách!", UiState.SyncState.SYNCED));
                                loadStatus(userId);
                            }

                            @Override
                            public void onError(Exception e) {
                                submitState.setValue(UiState.error("Lưu hồ sơ thất bại: "
                                        + (e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")));
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        submitState.setValue(UiState.error("Không thể định vị địa chỉ này qua bản đồ. Vui lòng kiểm tra lại số nhà, tên đường hoặc Phường/Quận."));
                    }
                });
    }
}
