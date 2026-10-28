package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.remote.VerificationTicketRemote;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.NetworkUtils;

public class VerificationRepository {
    private final Context ctx;
    private final VerificationTicketDao local;
    private final VerificationTicketRemote remote;

    public VerificationRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new VerificationTicketDao(this.ctx);
        this.remote = new VerificationTicketRemote();
    }

    public void submitTicket(String userId, String ticketType, String fileUrl, String metadataJson,
                             AppCallback<VerificationTicket> cb) {
        VerificationTicket ticket = new VerificationTicket(userId, ticketType, fileUrl, metadataJson,
                Constants.VERIF_APPROVED, System.currentTimeMillis());
        ticket.setTicketId("ticket_" + System.currentTimeMillis());
        ticket.setReviewedAt(System.currentTimeMillis());

        local.upsert(ticket);

        if (NetworkUtils.isOnline(ctx)) {
            remote.submit(ticket, new AppCallback<String>() {
                @Override
                public void onSuccess(String ticketId) {
                    ticket.setTicketId(ticketId);
                    ticket.setSyncStatus(Constants.SYNC_SYNCED);
                    local.upsert(ticket);
                    if (cb != null) cb.onSuccess(ticket);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(ticket);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(ticket);
        }
    }
}
