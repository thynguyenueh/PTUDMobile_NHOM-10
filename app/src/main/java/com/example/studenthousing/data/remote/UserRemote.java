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

        String uid = user.getUserId();

        // 1. Tách thông tin công khai ghi vào users/{uid}
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

        // 2. Tách thông tin riêng tư nhạy cảm ghi vào subcollection users/{uid}/private/sensitive_data
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

                        // Cố gắng đọc subcollection sensitive_data nếu có quyền
                        db.collection(COLLECTION).document(userId)
                                .collection(PRIVATE_SUBCOLLECTION).document(SENSITIVE_DOC)
                                .get()
                                .addOnSuccessListener(privDoc -> {
                                    if (privDoc != null && privDoc.exists() && user != null) {
                                        if (privDoc.contains("phone")) user.setPhone(privDoc.getString("phone"));
                                        if (privDoc.contains("citizenIdHash")) user.setCitizenIdHash(privDoc.getString("citizenIdHash"));
                                        if (privDoc.contains("permanentAddress")) user.setPermanentAddress(privDoc.getString("permanentAddress"));
                                    }
                                    if (cb != null) cb.onSuccess(user);
                                })
                                .addOnFailureListener(e -> {
                                    // Không có quyền đọc private data -> trả về public profile
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
