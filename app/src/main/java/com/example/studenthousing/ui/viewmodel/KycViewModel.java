package com.example.studenthousing.ui.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.data.repository.VerificationRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KycViewModel extends AndroidViewModel {

    private final SessionManager session;
    private final UserRepository userRepo;
    private final VerificationRepository verificationRepo;
    private final MutableLiveData<UiState<Boolean>> submitState = new MutableLiveData<>(UiState.content(false, UiState.SyncState.SYNCED));

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public KycViewModel(@NonNull Application application) {
        super(application);
        session = new SessionManager(application);
        userRepo = new UserRepository(application);
        verificationRepo = new VerificationRepository(application);
    }

    public LiveData<UiState<Boolean>> getSubmitState() {
        return submitState;
    }

    public void submitKyc(Uri parentFront, Uri parentBack, Uri studentFront, Uri studentBack, boolean consent) {
        boolean hasParentComplete = parentFront != null && parentBack != null;
        boolean hasParentPartial = (parentFront != null || parentBack != null) && !hasParentComplete;

        boolean hasStudentComplete = studentFront != null && studentBack != null;
        boolean hasStudentPartial = (studentFront != null || studentBack != null) && !hasStudentComplete;

        if (hasParentPartial) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ cả mặt trước và mặt sau Căn cước công dân"));
            return;
        }
        if (hasStudentPartial) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ cả mặt trước và mặt sau Thẻ sinh viên"));
            return;
        }
        if (!hasParentComplete && !hasStudentComplete) {
            submitState.setValue(UiState.error("Vui lòng chọn đủ 2 mặt của CCCD Phụ huynh hoặc Thẻ sinh viên"));
            return;
        }
        if (!consent) {
            submitState.setValue(UiState.error("Vui lòng đồng ý với điều khoản cam kết thông tin"));
            return;
        }

        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            submitState.setValue(UiState.error("Không tìm thấy tài khoản. Vui lòng đăng nhập lại."));
            return;
        }

        submitState.setValue(UiState.loading());

        ioExecutor.execute(() -> {
            try {
                // ✅ Copy ảnh sang thư mục riêng để URI không hết hạn
                String frontUrl = hasParentComplete ? copyToInternalStorage(parentFront, userId, "cccd_front") : null;
                String backUrl = hasParentComplete ? copyToInternalStorage(parentBack, userId, "cccd_back") : null;
                String studentFrontUrl = hasStudentComplete ? copyToInternalStorage(studentFront, userId, "student_front") : null;
                String studentBackUrl = hasStudentComplete ? copyToInternalStorage(studentBack, userId, "student_back") : null;

                String studentCardUrl = null;
                if (studentFrontUrl != null && studentBackUrl != null) {
                    studentCardUrl = studentFrontUrl + "|" + studentBackUrl;
                } else if (studentFrontUrl != null) {
                    studentCardUrl = studentFrontUrl;
                }

                List<String> allUrls = new ArrayList<>();
                if (frontUrl != null) allUrls.add(frontUrl);
                if (backUrl != null) allUrls.add(backUrl);
                if (studentFrontUrl != null) allUrls.add(studentFrontUrl);
                if (studentBackUrl != null) allUrls.add(studentBackUrl);
                String fileUrls = String.join("|", allUrls);

                final String finalFrontUrl = frontUrl;
                final String finalBackUrl = backUrl;
                final String finalStudentCardUrl = studentCardUrl;

                verificationRepo.submitTicket(userId, Constants.TICKET_CITIZEN, fileUrls, null, new AppCallback<VerificationTicket>() {
                    @Override
                    public void onSuccess(VerificationTicket ticket) {
                        userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, finalFrontUrl, finalBackUrl, finalStudentCardUrl, new AppCallback<Void>() {
                            @Override
                            public void onSuccess(Void d) {
                                mainHandler.post(() -> submitState.setValue(UiState.content(true, UiState.SyncState.SYNCED)));
                            }
                            @Override
                            public void onError(Exception e) {
                                mainHandler.post(() -> submitState.setValue(UiState.content(true, UiState.SyncState.SYNCED)));
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, finalFrontUrl, finalBackUrl, finalStudentCardUrl, new AppCallback<Void>() {
                            @Override
                            public void onSuccess(Void d) {
                                mainHandler.post(() -> submitState.setValue(UiState.content(true, UiState.SyncState.SYNCED)));
                            }
                            @Override
                            public void onError(Exception ex) {
                                mainHandler.post(() -> submitState.setValue(UiState.content(true, UiState.SyncState.SYNCED)));
                            }
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> submitState.setValue(UiState.error("Lỗi lưu ảnh: " + e.getMessage())));
            }
        });
    }

    private String copyToInternalStorage(Uri uri, String userId, String prefix) {
        if (uri == null) return null;
        try {
            // Nếu đã là file:/// thì giữ nguyên
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                File f = new File(uri.getPath());
                if (f.exists()) return uri.toString();
            }

            File dir = new File(getApplication().getFilesDir(), "kyc_docs");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new Exception("Không thể tạo thư mục KYC");
            }
            File outFile = new File(dir, userId + "_" + prefix + "_" + System.currentTimeMillis() + ".jpg");

            try (InputStream in = getApplication().getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(outFile)) {
                if (in == null) throw new Exception("Không đọc được file");
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            return Uri.fromFile(outFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return uri.toString();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}