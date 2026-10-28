package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;

public class VerifiedAddressRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "verified_addresses";

    public void save(VerifiedAddress addr, AppCallback<Void> cb) {
        String docId = addr.getFingerprint();
        if (docId == null || docId.isEmpty()) {
            docId = addr.getAddressId();
        }
        if (docId == null || docId.isEmpty()) {
            docId = db.collection(COLLECTION).document().getId();
            addr.setAddressId(docId);
        }
        db.collection(COLLECTION).document(docId)
                .set(addr)
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getByFingerprint(String fingerprint, AppCallback<VerifiedAddress> cb) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            cb.onSuccess(null);
            return;
        }
        db.collection(COLLECTION).document(fingerprint)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        VerifiedAddress addr = doc.toObject(VerifiedAddress.class);
                        if (addr != null) addr.setAddressId(doc.getId());
                        cb.onSuccess(addr);
                    } else {
                        cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void getByOwner(String ownerUserId, AppCallback<java.util.List<VerifiedAddress>> cb) {
        if (ownerUserId == null || ownerUserId.isEmpty()) {
            cb.onSuccess(new java.util.ArrayList<>());
            return;
        }
        db.collection(COLLECTION)
                .whereEqualTo("ownerUserId", ownerUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.List<VerifiedAddress> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        VerifiedAddress addr = doc.toObject(VerifiedAddress.class);
                        if (addr != null) addr.setAddressId(doc.getId());
                        list.add(addr);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }
}
