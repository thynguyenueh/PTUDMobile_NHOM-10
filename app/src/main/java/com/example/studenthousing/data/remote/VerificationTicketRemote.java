package com.example.studenthousing.data.remote;

import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.util.AppCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VerificationTicketRemote {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "verification_tickets";

    public void submit(VerificationTicket ticket, AppCallback<String> cb) {
        String id = ticket.getTicketId();
        if (id == null || id.isEmpty()) {
            id = db.collection(COLLECTION).document().getId();
            ticket.setTicketId(id);
        }
        final String ticketId = id;
        db.collection(COLLECTION).document(ticketId)
                .set(ticket)
                .addOnSuccessListener(a -> cb.onSuccess(ticketId))
                .addOnFailureListener(cb::onError);
    }

    public void updateStatus(String ticketId, String status, AppCallback<Void> cb) {
        db.collection(COLLECTION).document(ticketId)
                .update("status", status, "reviewedAt", System.currentTimeMillis())
                .addOnSuccessListener(a -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getAllByUser(String userId, AppCallback<List<VerificationTicket>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<VerificationTicket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        VerificationTicket t = doc.toObject(VerificationTicket.class);
                        if (t != null) t.setTicketId(doc.getId());
                        list.add(t);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }
}
