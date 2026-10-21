package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;

public class VerificationTicket {
    @DocumentId
    private String ticketId;
    private String userId;
    private String ticketType;   // STUDENT | CITIZEN | RESIDENCE
    private String method;       // FILE | MANUAL
    private String fileUrl;      // Cloudinary / file URL
    private String metadataJson; // JSON metadata
    private String status;       // PENDING | APPROVED | REJECTED
    private long submittedAt;
    private long reviewedAt;
    private String syncStatus = Constants.SYNC_SYNCED;
    private long cachedAt;

    public VerificationTicket() {}

    public VerificationTicket(String userId, String ticketType, String fileUrl, String metadataJson, String status, long submittedAt) {
        this.userId = userId;
        this.ticketType = ticketType;
        this.fileUrl = fileUrl;
        this.metadataJson = metadataJson;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedAt = 0L;
    }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getRegNumber() {
        if (metadataJson != null && metadataJson.contains("regNumber")) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(metadataJson);
                return obj.optString("regNumber", "");
            } catch (Exception ignored) {}
        }
        return "";
    }

    public String getWard() {
        if (metadataJson != null && metadataJson.contains("ward")) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(metadataJson);
                return obj.optString("ward", "");
            } catch (Exception ignored) {}
        }
        return "";
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public long getCachedAt() { return cachedAt; }
    public void setCachedAt(long cachedAt) { this.cachedAt = cachedAt; }
}
