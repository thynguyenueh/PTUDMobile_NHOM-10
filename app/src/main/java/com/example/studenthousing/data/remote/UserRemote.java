package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "users";
    private static final String PRIVATE_SUBCOLLECTION = "private";
    private static final String SENSITIVE_DOC = "sensitive_data";

    public void saveUser(User user, AppCallback<Void> cb) {
        if (user == null || user.getUserId() == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("User or UserId is null"));
            return;
        }

        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    executeSaveUser(user, cb);
                } else {
                    if (cb != null) cb.onError(task.getException() != null ? task.getException() : new Exception("Auth failed"));
                }
            });
        } else {
            executeSaveUser(user, cb);
        }
    }

    private void executeSaveUser(User user, AppCallback<Void> cb) {
        String uid = user.getUserId();

        // 1. Thông tin công khai
        Map<String, Object> publicData = new HashMap<>();
        publicData.put("familyName", user.getFamilyName());
        publicData.put("avatarUrl", user.getAvatarUrl());
        publicData.put("identityType", user.getIdentityType());
        publicData.put("identityVerified", user.isIdentityVerified());
        publicData.put("identityVerifiedAt", user.getIdentityVerifiedAt());
        publicData.put("universityCode", user.getUniversityCode());
        publicData.put("universityName", user.getUniversityName());
        publicData.put("residenceVerified", user.isResidenceVerified());
        publicData.put("residenceVerifiedAt", user.getResidenceVerifiedAt());
        publicData.put("fullyVerified", user.isFullyVerified());
        publicData.put("createdAt", user.getCreatedAt());
        publicData.put("updatedAt", System.currentTimeMillis());
        publicData.put("phoneVerified", user.isPhoneVerified());
        publicData.put("isVerified", user.isVerified());

        // 2. Thông tin nhạy cảm (bao gồm 3 trường ảnh KYC)
        Map<String, Object> sensitiveData = new HashMap<>();
        if (user.getPhone() != null) sensitiveData.put("phone", user.getPhone());
        if (user.getCitizenIdHash() != null) sensitiveData.put("citizenIdHash", user.getCitizenIdHash());
        if (user.getCitizenIdFrontUrl() != null) sensitiveData.put("citizenIdFrontUrl", user.getCitizenIdFrontUrl());
        if (user.getCitizenIdBackUrl() != null) sensitiveData.put("citizenIdBackUrl", user.getCitizenIdBackUrl());
        if (user.getStudentCardUrl() != null) sensitiveData.put("studentCardUrl", user.getStudentCardUrl());
        if (user.getPermanentAddress() != null) sensitiveData.put("permanentAddress", user.getPermanentAddress());
        sensitiveData.put("permanentCityCode", user.getPermanentCityCode());
        sensitiveData.put("permanentWardCode", user.getPermanentWardCode());
        if (user.getPermanentCityName() != null) sensitiveData.put("permanentCityName", user.getPermanentCityName());
        if (user.getPermanentWardName() != null) sensitiveData.put("permanentWardName", user.getPermanentWardName());

        // Ghi đè lên Firestore (không merge cho sensitiveData để tránh lẫn dữ liệu cũ)
        db.collection(COLLECTION).document(uid)
                .set(publicData, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    db.collection(COLLECTION).document(uid)
                            .collection(PRIVATE_SUBCOLLECTION).document(SENSITIVE_DOC)
                            .set(sensitiveData, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (cb != null) cb.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (cb != null) cb.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void getUser(String userId, AppCallback<User> cb) {
        if (userId == null) {
            if (cb != null) cb.onSuccess(null);
            return;
        }

        db.collection(COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) user.setUserId(doc.getId());

                        // ✅ Đọc sensitive_data để lấy lại 3 trường ảnh KYC
                        db.collection(COLLECTION).document(userId)
                                .collection(PRIVATE_SUBCOLLECTION).document(SENSITIVE_DOC)
                                .get()
                                .addOnSuccessListener(privDoc -> {
                                    if (privDoc != null && privDoc.exists() && user != null) {
                                        if (privDoc.contains("phone")) user.setPhone(privDoc.getString("phone"));
                                        if (privDoc.contains("citizenIdHash")) user.setCitizenIdHash(privDoc.getString("citizenIdHash"));
                                        if (privDoc.contains("permanentAddress")) user.setPermanentAddress(privDoc.getString("permanentAddress"));

                                        // ✅ FIX: Đọc 3 trường ảnh KYC
                                        if (privDoc.contains("citizenIdFrontUrl")) user.setCitizenIdFrontUrl(privDoc.getString("citizenIdFrontUrl"));
                                        if (privDoc.contains("citizenIdBackUrl")) user.setCitizenIdBackUrl(privDoc.getString("citizenIdBackUrl"));
                                        if (privDoc.contains("studentCardUrl")) user.setStudentCardUrl(privDoc.getString("studentCardUrl"));
                                    }
                                    if (cb != null) cb.onSuccess(user);
                                })
                                .addOnFailureListener(e -> {
                                    if (cb != null) cb.onSuccess(user);
                                });
                    } else {
                        if (cb != null) cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }
}