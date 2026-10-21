package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import java.util.List;

public class AccommodationPost {
    @DocumentId
    private String postId;
    private String userId;
    private String familyName;
    private String title;
    private String address;
    private String addressFingerprint;
    private String addressVerificationStatus = Constants.ADDR_UNVERIFIED;
    private String roomNumber;
    private int cityCode;
    private int wardCode;
    private String cityName;
    private String wardName;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private String distanceSource;
    private boolean isDistanceEstimate;
    private String listingType = Constants.LISTING_EXCHANGE;
    private String houseType;
    private double price;
    private double area;
    private String description;
    private List<String> images;
    private List<String> utilities;
    private boolean isPoliceVerified;
    private String verificationBadge;
    private String status = Constants.POST_AVAILABLE;
    private long createdAt;
    private long updatedAt;
    private String syncStatus = Constants.SYNC_SYNCED;

    public AccommodationPost() {}

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAddressFingerprint() { return addressFingerprint; }
    public void setAddressFingerprint(String addressFingerprint) { this.addressFingerprint = addressFingerprint; }

    public String getAddressVerificationStatus() { return addressVerificationStatus; }
    public void setAddressVerificationStatus(String addressVerificationStatus) { this.addressVerificationStatus = addressVerificationStatus; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getCityCode() { return cityCode; }
    public void setCityCode(int cityCode) { this.cityCode = cityCode; }

    public int getWardCode() { return wardCode; }
    public void setWardCode(int wardCode) { this.wardCode = wardCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    @Exclude
    public String getProvince() { return cityName; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    @Exclude
    public String getWard() { return wardName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getDistanceSource() { return distanceSource; }
    public void setDistanceSource(String distanceSource) { this.distanceSource = distanceSource; }

    public boolean isDistanceEstimate() { return isDistanceEstimate; }
    public void setDistanceEstimate(boolean distanceEstimate) { isDistanceEstimate = distanceEstimate; }

    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }

    public String getHouseType() { return houseType; }
    public void setHouseType(String houseType) { this.houseType = houseType; }

    @Exclude
    public String getHousingType() { return houseType; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getUtilities() { return utilities; }
    public void setUtilities(List<String> utilities) { this.utilities = utilities; }

    public boolean isPoliceVerified() { return isPoliceVerified; }
    public void setPoliceVerified(boolean policeVerified) { isPoliceVerified = policeVerified; }

    public String getVerificationBadge() { return verificationBadge; }
    public void setVerificationBadge(String verificationBadge) { this.verificationBadge = verificationBadge; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Exclude
    public String getHouseTypeDisplay() {
        if (houseType == null) return "";
        switch (houseType.toUpperCase()) {
            case "APARTMENT":    return "Căn hộ";
            case "WHOLE_HOUSE":  return "Nhà nguyên căn";
            case "SHARED_ROOM":  return "Phòng ở ghép";
            default:             return houseType;
        }
    }
}
