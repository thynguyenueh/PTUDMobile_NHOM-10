package com.example.studenthousing.data.model;

public class VerifiedAddress {
    private String addressId;
    private String fingerprint;
    private int cityCode;
    private int wardCode;
    private String streetNormalized;
    private String houseNumberNorm;
    private String fullAddress;
    private String ownerUserId;
    private String verificationTicketId;
    private long verifiedAt;
    private String status;
    private long cachedAt;

    public VerifiedAddress() {}

    public VerifiedAddress(String addressId, String fullAddress, String ownerUserId,
                           int cityCode, int wardCode, String ticketId) {
        this.addressId = addressId;
        this.fullAddress = fullAddress;
        this.ownerUserId = ownerUserId;
        this.cityCode = cityCode;
        this.wardCode = wardCode;
        this.verificationTicketId = ticketId;
        this.status = "APPROVED";
        this.verifiedAt = System.currentTimeMillis();
        this.cachedAt = System.currentTimeMillis();
    }

    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public int getCityCode() { return cityCode; }
    public void setCityCode(int cityCode) { this.cityCode = cityCode; }

    public int getWardCode() { return wardCode; }
    public void setWardCode(int wardCode) { this.wardCode = wardCode; }

    public String getStreetNormalized() { return streetNormalized; }
    public void setStreetNormalized(String streetNormalized) { this.streetNormalized = streetNormalized; }

    public String getHouseNumberNorm() { return houseNumberNorm; }
    public void setHouseNumberNorm(String houseNumberNorm) { this.houseNumberNorm = houseNumberNorm; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getVerificationTicketId() { return verificationTicketId; }
    public void setVerificationTicketId(String verificationTicketId) { this.verificationTicketId = verificationTicketId; }

    public long getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(long verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCachedAt() { return cachedAt; }
    public void setCachedAt(long cachedAt) { this.cachedAt = cachedAt; }
}
