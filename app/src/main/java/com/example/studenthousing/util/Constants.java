package com.example.studenthousing.util;

public final class Constants {
    private Constants() {}

    // Identity Types
    public static final String IDENTITY_NONE = "NONE";
    public static final String IDENTITY_STUDENT = "STUDENT";
    public static final String IDENTITY_CITIZEN = "CITIZEN";
    // Verification Statuses
    public static final String VERIF_NONE = "NONE";
    public static final String VERIF_PENDING = "PENDING";
    public static final String VERIF_APPROVED = "APPROVED";
    public static final String VERIF_REJECTED = "REJECTED";

    // Ticket Types
    public static final String TICKET_STUDENT = "STUDENT";
    public static final String TICKET_CITIZEN = "CITIZEN";
    public static final String TICKET_RESIDENCE = "RESIDENCE";

    // Post Statuses
    public static final String POST_AVAILABLE = "AVAILABLE";
    public static final String POST_EXCHANGED = "EXCHANGED";

    // Address Verification Statuses
    public static final String ADDR_UNVERIFIED = "UNVERIFIED";
    public static final String ADDR_VERIFIED = "VERIFIED";
    public static final String ADDR_DISPUTED = "DISPUTED";

    // Request Statuses
    public static final String REQ_PENDING = "PENDING";
    public static final String REQ_EXCHANGING = "EXCHANGING";
    public static final String REQ_REJECTED = "REJECTED";
    public static final String REQ_CANCELLED = "CANCELLED";
    public static final String REQ_WITHDRAWN = "WITHDRAWN";
    public static final String REQ_EXPIRED = "EXPIRED";
    public static final String REQ_COMPLETED = "COMPLETED";

    // Reject reasons (khớp chuỗi trong firestore.rules)
    public static final String REJECT_REASON_TAKEN = "Bài đăng đã có người chốt";
    public static final String REJECT_REASON_EXPIRED = "Hết hạn phản hồi sau 7 ngày";
    public static final String REJECT_REASON_CANCEL_SILENT = "Đối tác không phản hồi yêu cầu huỷ sau 3 ngày";

    // Thời gian auto-transition
    public static final long PENDING_EXPIRE_MS = 7L * 24 * 3600 * 1000;
    public static final long CANCEL_SILENT_MS = 3L * 24 * 3600 * 1000;

    // Địa lý TPHCM
    public static final int TPHCM_CITY_CODE = 79;
    public static final String TPHCM_CITY_NAME = "TP. Hồ Chí Minh";

    // Sync Statuses
    public static final String SYNC_SYNCED = "SYNCED";
    public static final String SYNC_SYNCING = "SYNCING";
    public static final String SYNC_PENDING_CREATE = "PENDING_CREATE";
    public static final String SYNC_PENDING_UPDATE = "PENDING_UPDATE";
    public static final String SYNC_PENDING_DELETE = "PENDING_DELETE";
}
