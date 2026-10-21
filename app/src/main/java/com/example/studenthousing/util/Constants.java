package com.example.studenthousing.util;

public final class Constants {
    private Constants() {}

    // Identity Types
    public static final String IDENTITY_NONE = "NONE";
    public static final String IDENTITY_STUDENT = "STUDENT";
    public static final String IDENTITY_CITIZEN = "CITIZEN";
    public static final String IDENTITY_BOTH = "BOTH";

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
    public static final String POST_HIDDEN = "HIDDEN";
    public static final String POST_RENTED = "RENTED";

    // Address Verification Statuses
    public static final String ADDR_UNVERIFIED = "UNVERIFIED";
    public static final String ADDR_VERIFIED = "VERIFIED";
    public static final String ADDR_DISPUTED = "DISPUTED";

    // Listing Types
    public static final String LISTING_EXCHANGE = "EXCHANGE";
    public static final String LISTING_RENT_OUT = "RENT_OUT";

    // Request Statuses
    public static final String REQ_PENDING = "PENDING";
    public static final String REQ_ACCEPTED = "ACCEPTED";
    public static final String REQ_REJECTED = "REJECTED";
    public static final String REQ_CANCELLED = "CANCELLED";
    public static final String REQ_COMPLETED = "COMPLETED";

    // Sync Statuses
    public static final String SYNC_SYNCED = "SYNCED";
    public static final String SYNC_PENDING_CREATE = "PENDING_CREATE";
    public static final String SYNC_PENDING_UPDATE = "PENDING_UPDATE";
    public static final String SYNC_PENDING_DELETE = "PENDING_DELETE";
}
