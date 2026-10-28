package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;

public class AuthViewModel extends AndroidViewModel {

    public static class GoogleAuthResult {
        public final User user;
        public final boolean isRegistrationRequired;

        public GoogleAuthResult(User user, boolean isRegistrationRequired) {
            this.user = user;
            this.isRegistrationRequired = isRegistrationRequired;
        }
    }

    private final UserRepository userRepository;
    private final SessionManager session;

    private final MutableLiveData<UiState<GoogleAuthResult>> googleAuthState = new MutableLiveData<>(UiState.content(null, UiState.SyncState.SYNCED));

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        session = new SessionManager(application);
    }

    public LiveData<UiState<GoogleAuthResult>> getGoogleAuthState() { return googleAuthState; }

    public void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) return;
        googleAuthState.setValue(UiState.loading());
        FirebaseAuth auth = FirebaseAuth.getInstance();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                googleAuthState.setValue(UiState.error("Xác thực Firebase thất bại"));
                return;
            }
            FirebaseUser fbUser = auth.getCurrentUser();
            if (fbUser == null) {
                googleAuthState.setValue(UiState.error("Không lấy được người dùng Firebase"));
                return;
            }

            User partial = new User();
            partial.setUserId(fbUser.getUid());
            partial.setFamilyName(fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Người dùng Google");
            partial.setAvatarUrl(fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : null);

            userRepository.upsertAuthProfile(partial, new AppCallback<User>() {
                @Override
                public void onSuccess(User merged) {
                    boolean incomplete = merged.getPhone() == null || merged.getPhone().trim().isEmpty();
                    if (!incomplete) {
                        session.createLoginSession(merged);
                    }
                    googleAuthState.postValue(UiState.content(new GoogleAuthResult(merged, incomplete), UiState.SyncState.SYNCED));
                }

                @Override
                public void onError(Exception e) {
                    auth.signOut();
                    googleAuthState.postValue(UiState.error("Không kết nối được máy chủ, vui lòng thử lại"));
                }
            });
        });
    }

    public void firebaseAuthWithPhone(PhoneAuthCredential credential) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        googleAuthState.setValue(UiState.loading());

        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                googleAuthState.setValue(UiState.error("Xác thực SĐT thất bại"));
                return;
            }
            FirebaseUser fbUser = auth.getCurrentUser();
            if (fbUser == null) {
                googleAuthState.setValue(UiState.error("Không lấy được user"));
                return;
            }

            User partial = new User();
            partial.setUserId(fbUser.getUid());
            partial.setPhone(fbUser.getPhoneNumber());

            userRepository.upsertAuthProfile(partial, new AppCallback<User>() {
                @Override
                public void onSuccess(User merged) {
                    session.createLoginSession(merged);
                    googleAuthState.postValue(UiState.content(
                            new GoogleAuthResult(merged, false), UiState.SyncState.SYNCED));
                }

                @Override
                public void onError(Exception e) {
                    auth.signOut();
                    googleAuthState.postValue(UiState.error("Lỗi tạo profile: " + e.getMessage()));
                }
            });
        });
    }
}
