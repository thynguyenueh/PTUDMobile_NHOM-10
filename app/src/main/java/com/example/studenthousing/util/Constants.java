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

    // Address Validation Statuses
    public static final String ADDR_UNCHECKED = "UNCHECKED";
    public static final String ADDR_VALID = "VALID";
    public static final String ADDR_INVALID = "INVALID";

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
