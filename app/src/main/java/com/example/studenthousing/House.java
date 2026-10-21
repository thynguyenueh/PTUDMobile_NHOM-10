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
    private final String houseType;

    public House(@NonNull String title,
                 @NonNull String address,
                 double distanceKm,
                 boolean verified,
                 @DrawableRes int photoRes) {
        this("house_" + System.currentTimeMillis(), title, address, distanceKm, verified, photoRes, 3.5, 45.0, "Căn hộ rộng rãi, thoáng mát, đầy đủ tiện nghi.", "Hộ gia đình Nguyễn Văn A", "Căn hộ");
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
    }

    public String getPostId() {
        return postId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public boolean isVerified() {
        return verified;
    }

    @DrawableRes
    public int getPhotoRes() {
        return photoRes;
    }

    public double getPrice() {
        return price;
    }

    public double getArea() {
        return area;
    }

    public String getDescription() {
        return description;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHouseType() {
        return houseType;
    }
}
