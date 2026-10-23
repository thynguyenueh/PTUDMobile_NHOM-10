package com.example.studenthousing;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/** Thông tin một căn nhà hiển thị ở danh sách "Nhà gần trường". */
public final class House {

    private final String postId;
    @NonNull
    private final String title;
    @NonNull
    private final String address;
    private final double distanceKm;
    private final boolean verified;
    @DrawableRes
    private final int photoRes;
    private final double price;
    private final double area;
    private final String description;
    private final String hostName;
    private final String houseType;      // APARTMENT | WHOLE_HOUSE | SHARED_ROOM
    private final String status;         // AVAILABLE | EXCHANGED | HIDDEN
    private final String verificationBadge; // "Đã xác thực Công an Phường"
    private final boolean isDistanceEstimate; // true = Haversine, false = OSRM
    private final String distanceSource; // "osrm" | "ors" | "haversine"

    public House(@NonNull String title,
                 @NonNull String address,
                 double distanceKm,
                 boolean verified,
                 @DrawableRes int photoRes) {
        this("house_" + System.currentTimeMillis(), title, address, distanceKm, verified, photoRes, 3.5, 45.0, "Căn hộ rộng rãi, thoáng mát, đầy đủ tiện nghi.", "Hộ gia đình Nguyễn Văn A", "APARTMENT", "AVAILABLE", "Đã xác thực Công an Phường", false, "osrm");
    }

    public House(String postId,
                 @NonNull String title,
                 @NonNull String address,
                 double distanceKm,
                 boolean verified,
                 @DrawableRes int photoRes,
                 double price,
                 double area,
                 String description,
                 String hostName,
                 String houseType) {
        this(postId, title, address, distanceKm, verified, photoRes, price, area, description, hostName, houseType, "AVAILABLE", "Đã xác thực Công an Phường", false, "osrm");
    }

    public House(String postId,
                 @NonNull String title,
                 @NonNull String address,
                 double distanceKm,
                 boolean verified,
                 @DrawableRes int photoRes,
                 double price,
                 double area,
                 String description,
                 String hostName,
                 String houseType,
                 String status,
                 String verificationBadge,
                 boolean isDistanceEstimate,
                 String distanceSource) {
        this.postId = postId;
        this.title = title;
        this.address = address;
        this.distanceKm = distanceKm;
        this.verified = verified;
        this.photoRes = photoRes;
        this.price = price;
        this.area = area;
        this.description = description;
        this.hostName = hostName;
        this.houseType = houseType;
        this.status = status;
        this.verificationBadge = verificationBadge;
        this.isDistanceEstimate = isDistanceEstimate;
        this.distanceSource = distanceSource;
    }

    public String getPostId() { return postId; }

    @NonNull
    public String getTitle() { return title; }

    @NonNull
    public String getAddress() { return address; }

    public double getDistanceKm() { return distanceKm; }

    public boolean isVerified() { return verified; }

    @DrawableRes
    public int getPhotoRes() { return photoRes; }

    public double getPrice() { return price; }

    public double getArea() { return area; }

    public String getDescription() { return description; }

    public String getHostName() { return hostName; }

    public String getHouseType() { return houseType; }

    public String getStatus() { return status; }

    public String getVerificationBadge() { return verificationBadge; }

    public boolean isDistanceEstimate() { return isDistanceEstimate; }

    public String getDistanceSource() { return distanceSource; }

    public String getHouseTypeDisplay() {
        if (houseType == null) return "Căn hộ";
        switch (houseType.toUpperCase()) {
            case "APARTMENT":    return "Căn hộ";
            case "WHOLE_HOUSE":  return "Nhà nguyên căn";
            case "SHARED_ROOM":  return "Phòng ở ghép";
            default:             return houseType;
        }
    }

    public String getStatusDisplay() {
        if (status == null) return "Còn trống";
        switch (status.toUpperCase()) {
            case "AVAILABLE": return "Còn trống";
            case "EXCHANGED": return "Đã đổi trọ";
            case "HIDDEN":    return "Đã ẩn";
            default:          return status;
        }
    }

    public String getDistanceDisplay() {
        if (isDistanceEstimate) {
            return "~ " + distanceKm + " km (Ước lượng)";
        }
        return distanceKm + " km · ĐHQG TP.HCM";
    }
}
