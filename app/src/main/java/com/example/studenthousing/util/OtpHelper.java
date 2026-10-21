package com.example.studenthousing.util;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.example.studenthousing.BuildConfig;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public final class OtpHelper {

    public interface SendCallback {
        void onCodeSent(String verificationId);
        void onFailed(String message);
    }

    private OtpHelper() {}

    public static String toLocal(String phone) {
        String p = phone == null ? "" : phone.replaceAll("[\\s.\\-()]", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        else if (p.startsWith("84") && p.length() > 9) p = "0" + p.substring(2);
        return p;
    }

    public static String toE164(String phone) {
        String p = toLocal(phone);
        return p.startsWith("0") ? "+84" + p.substring(1) : p;
    }

    public static boolean isValidVnPhone(String phone) {
        return toLocal(phone).matches("0\\d{9}");
    }

    public static void send(@NonNull Activity activity, String phone, @NonNull SendCallback cb) {
        if (BuildConfig.DEBUG) {
            FirebaseAuth.getInstance().getFirebaseAuthSettings()
                    .setAppVerificationDisabledForTesting(true);
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(toE164(phone))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        cb.onFailed("Lỗi gửi OTP: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        cb.onCodeSent(verificationId);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}
