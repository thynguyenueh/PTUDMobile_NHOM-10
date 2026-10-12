package com.example.studenthousing.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class AccommodationPost {
    @DocumentId
    private String postId;
    private String userId;
    private String familyName;         // denormalize
    private String title;
    private String address;
    private int cityCode;
    private int districtCode;
    private int wardCode;
    private String cityName;
    private String districtName;
    private String wardName;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private String houseType;          // APARTMENT | WHOLE_HOUSE | SHARED_ROOM
    private double price;
    private double area;
    private String description;
    private List<String> images;       // URL hoặc resource name
    private List<String> utilities;
    private boolean isPoliceVerified;
    private String verificationBadge;
    private String status;             // AVAILABLE | EXCHANGED | HIDDEN
    private long createdAt;
    private long updatedAt;

    public AccommodationPost() {}

    // Getters & Setters
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

    public int getCityCode() { return cityCode; }
    public void setCityCode(int cityCode) { this.cityCode = cityCode; }

    public int getDistrictCode() { return districtCode; }
    public void setDistrictCode(int districtCode) { this.districtCode = districtCode; }

    public int getWardCode() { return wardCode; }
    public void setWardCode(int wardCode) { this.wardCode = wardCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getHouseType() { return houseType; }
    public void setHouseType(String houseType) { this.houseType = houseType; }

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
}
