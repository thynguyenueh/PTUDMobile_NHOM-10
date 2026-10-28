package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;

public class ExchangeRequest {
    @DocumentId
    private String requestId;
    private String senderId;
    private String receiverId;
    private String senderPostId;
    private String receiverPostId;
    private String chatRoomId;
    private String exchangePeriod;
    private long startDate;
    private long endDate;
    private String message;
    private String agreementText;
    private String status = Constants.REQ_PENDING;
    private String cancelRequestedBy;
    private long cancelRequestedAt;
    private String rejectReason;
    private long createdAt;
    private long respondedAt;
    private long completedAt;
    private long updatedAt;
    private String syncStatus = Constants.SYNC_SYNCED;

    public ExchangeRequest() {}

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getSenderPostId() { return senderPostId; }
    public void setSenderPostId(String senderPostId) { this.senderPostId = senderPostId; }

    public String getReceiverPostId() { return receiverPostId; }
    public void setReceiverPostId(String receiverPostId) { this.receiverPostId = receiverPostId; }

    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

    public String getExchangePeriod() { return exchangePeriod; }
    public void setExchangePeriod(String exchangePeriod) { this.exchangePeriod = exchangePeriod; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAgreementText() { return agreementText; }
    public void setAgreementText(String agreementText) { this.agreementText = agreementText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelRequestedBy() { return cancelRequestedBy; }
    public void setCancelRequestedBy(String cancelRequestedBy) { this.cancelRequestedBy = cancelRequestedBy; }

    public long getCancelRequestedAt() { return cancelRequestedAt; }
    public void setCancelRequestedAt(long cancelRequestedAt) { this.cancelRequestedAt = cancelRequestedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getRespondedAt() { return respondedAt; }
    public void setRespondedAt(long respondedAt) { this.respondedAt = respondedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
