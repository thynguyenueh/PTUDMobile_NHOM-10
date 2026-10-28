package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
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
    private int cityCode = Constants.TPHCM_CITY_CODE;
    private int wardCode;
    private String cityName = Constants.TPHCM_CITY_NAME;
    private String wardName;
    private String streetName;
    @Exclude
    private String houseNumber;
    @Exclude
    private double latitude;
    @Exclude
    private double longitude;
    @Exclude
    private String verificationBadge;
    private List<NearbyCampus> nearbyCampuses;
    private List<String> nearbyCampusIds;
    private double distanceKm;
    private String distanceSource;
    private boolean isDistanceEstimate;
    private String description;
    private List<String> images;
    private List<String> utilities;
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

    public int getCityCode() { return cityCode; }
    public void setCityCode(int cityCode) { this.cityCode = cityCode; }

    public int getWardCode() { return wardCode; }
    public void setWardCode(int wardCode) { this.wardCode = wardCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getStreetName() { return streetName; }
    public void setStreetName(String streetName) { this.streetName = streetName; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getVerificationBadge() { return verificationBadge; }
    public void setVerificationBadge(String verificationBadge) { this.verificationBadge = verificationBadge; }

    @Exclude
    public String getProvince() { return cityName; }

    @Exclude
    public String getWard() { return wardName; }

    public List<NearbyCampus> getNearbyCampuses() { return nearbyCampuses; }
    public void setNearbyCampuses(List<NearbyCampus> nearbyCampuses) {
        this.nearbyCampuses = nearbyCampuses;
        if (nearbyCampuses != null) {
            List<String> ids = new ArrayList<>();
            for (NearbyCampus n : nearbyCampuses) {
                if (n.getCampusId() != null) ids.add(n.getCampusId());
            }
            this.nearbyCampusIds = ids;
        } else {
            this.nearbyCampusIds = null;
        }
    }

    public List<String> getNearbyCampusIds() { return nearbyCampusIds; }
    public void setNearbyCampusIds(List<String> nearbyCampusIds) { this.nearbyCampusIds = nearbyCampusIds; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getDistanceSource() { return distanceSource; }
    public void setDistanceSource(String distanceSource) { this.distanceSource = distanceSource; }

    public boolean isDistanceEstimate() { return isDistanceEstimate; }
    public void setDistanceEstimate(boolean isDistanceEstimate) { this.isDistanceEstimate = isDistanceEstimate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getUtilities() { return utilities; }
    public void setUtilities(List<String> utilities) { this.utilities = utilities; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
