package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.remote.ExchangeRemote;

import java.util.List;

/**
 * Kiểm tra phía client và tự động chuyển trạng thái:
 * 1. PENDING quá 7 ngày -> EXPIRED (lý do: REJECT_REASON_EXPIRED)
 * 2. EXCHANGING có end_date đã qua -> COMPLETED
 * 3. Yêu cầu hủy 1 phía không phản hồi quá 3 ngày -> CANCELLED (lý do: REJECT_REASON_CANCEL_SILENT)
 */
public class ExchangeStatusChecker {
    private static final String TAG = "ExchangeStatusChecker";

    public interface PostStatusChoiceCallback {
        void onChoiceRequired(ExchangeRequest request, PostStatusSelectionListener listener);
    }

    public interface PostStatusSelectionListener {
        void onSelected(String postStatus);
    }

    public static void checkAndApplyExpirations(Context context, List<ExchangeRequest> requests, String currentUserId, PostStatusChoiceCallback choiceCallback) {
        if (requests == null || requests.isEmpty()) return;
        long now = System.currentTimeMillis();
        ExchangeDao exchangeDao = new ExchangeDao(context);

        for (ExchangeRequest req : requests) {
            if (req == null || req.getStatus() == null) continue;
            String st = req.getStatus().toUpperCase();
            String previousStatus = req.getStatus();
            boolean updated = false;

            // 1. PENDING quá 7 ngày -> EXPIRED
            if (Constants.REQ_PENDING.equals(st)) {
                if (req.getCreatedAt() > 0 && (now - req.getCreatedAt() > Constants.PENDING_EXPIRE_MS)) {
                    req.setStatus(Constants.REQ_EXPIRED);
                    req.setRejectReason(Constants.REJECT_REASON_EXPIRED);
                    updated = true;
                }
            }
            // 2. EXCHANGING có end_date đã qua -> COMPLETED
            else if (Constants.REQ_EXCHANGING.equals(st)) {
                long endDate = req.getEndDate();
                if (endDate > 0 && now > endDate) {
                    req.setStatus(Constants.REQ_COMPLETED);
                    req.setCompletedAt(now);
                    updated = true;
                }
            }

            // 3. Yêu cầu hủy 1 phía không phản hồi quá 3 ngày -> CANCELLED
            if (req.getCancelRequestedBy() != null && req.getCancelRequestedAt() > 0
                    && (now - req.getCancelRequestedAt() > Constants.CANCEL_SILENT_MS)
                    && !Constants.REQ_CANCELLED.equals(st)
                    && !Constants.REQ_COMPLETED.equals(req.getStatus())
                    && !Constants.REQ_EXPIRED.equals(req.getStatus())) {
                req.setStatus(Constants.REQ_CANCELLED);
                req.setRejectReason(Constants.REJECT_REASON_CANCEL_SILENT);
                updated = true;
            }

            if (updated) {
                persistUpdatedStatus(req, previousStatus, exchangeDao);
            }
        }
    }

    private static void persistUpdatedStatus(ExchangeRequest request, String previousStatus, ExchangeDao exchangeDao) {
        String requestId = request.getRequestId();
        if (requestId == null || requestId.trim().isEmpty() || requestId.startsWith("temp_")) {
            exchangeDao.upsert(request);
            return;
        }

        String newStatus = request.getStatus();
        ExchangeRemote remote = new ExchangeRemote();
        remote.updateStatus(requestId, newStatus, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                request.setUpdatedAt(System.currentTimeMillis());
                exchangeDao.upsert(request);
            }

            @Override
            public void onError(Exception error) {
                request.setStatus(previousStatus);
                Log.e(TAG, "Could not sync exchange status for " + requestId, error);
            }
        });
    }
}
