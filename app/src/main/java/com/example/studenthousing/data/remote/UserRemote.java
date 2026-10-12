package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "users";

    public void saveUser(User user, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(user.getUserId())
                .set(user)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getUser(String userId, AppCallback<User> cb) {
        db.collection(COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) user.setUserId(doc.getId());
                        cb.onSuccess(user);
                    } else {
                        cb.onError(new Exception("Người dùng không tồn tại"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }
}
