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

    public void submitKyc(Uri parentFront, Uri parentBack, Uri studentId) {
        submitKyc(parentFront, parentBack, studentId, null, true);
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

        List<Uri> uris = new ArrayList<>();
        if (hasParentComplete) {
            uris.add(parentFront);
            uris.add(parentBack);
        }
        if (hasStudentComplete) {
            uris.add(studentFront);
            uris.add(studentBack);
        }

        uploadKycDocuments(uris, 0, new ArrayList<>(), userId, hasParentComplete, hasStudentComplete);
    }

    private void uploadKycDocuments(List<Uri> uris, int index, List<String> urls, String userId, boolean hasParent, boolean hasStudent) {
        if (index >= uris.size()) {
            saveTicketAndUser(userId, urls, hasParent, hasStudent);
            return;
        }
        File file = FileUtils.getFileFromUri(getApplication(), uris.get(index));
        if (file == null) {
            String fallbackUrl = uris.get(index).toString();
            urls.add(fallbackUrl);
            uploadKycDocuments(uris, index + 1, urls, userId, hasParent, hasStudent);
            return;
        }
        CloudinaryUploader.uploadImage(file, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) {
                urls.add(url);
                uploadKycDocuments(uris, index + 1, urls, userId, hasParent, hasStudent);
            }
            @Override public void onError(Exception e) {
                String fallbackUrl = Uri.fromFile(file).toString();
                urls.add(fallbackUrl);
                uploadKycDocuments(uris, index + 1, urls, userId, hasParent, hasStudent);
            }
        });
    }

    private void saveTicketAndUser(String userId, List<String> urls, boolean hasParent, boolean hasStudent) {
        final String finalFrontUrl;
        final String finalBackUrl;
        final String finalStudentCardUrl;

        int urlIndex = 0;
        if (hasParent && urls.size() >= 2) {
            finalFrontUrl = urls.get(urlIndex++);
            finalBackUrl = urls.get(urlIndex++);
        } else {
            finalFrontUrl = null;
            finalBackUrl = null;
        }

        if (hasStudent && urls.size() >= urlIndex + 2) {
            String studentFront = urls.get(urlIndex++);
            String studentBack = urls.get(urlIndex);
            finalStudentCardUrl = studentFront + "|" + studentBack;
        } else if (hasStudent && urls.size() >= urlIndex + 1) {
            finalStudentCardUrl = urls.get(urlIndex);
        } else {
            finalStudentCardUrl = null;
        }

        String fileUrls = String.join("|", urls);

        verificationRepo.submitTicket(userId, Constants.TICKET_CITIZEN, fileUrls, null, new AppCallback<VerificationTicket>() {
            @Override
            public void onSuccess(VerificationTicket ticket) {
                userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, finalFrontUrl, finalBackUrl, finalStudentCardUrl, new AppCallback<Void>() {
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
                userRepo.markIdentityVerified(userId, Constants.IDENTITY_CITIZEN, finalFrontUrl, finalBackUrl, finalStudentCardUrl, new AppCallback<Void>() {
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
