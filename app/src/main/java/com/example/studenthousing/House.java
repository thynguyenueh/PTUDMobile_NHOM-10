package com.example.studenthousing;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/** Thông tin một căn nhà hiển thị ở danh sách "Nhà gần trường". */
public final class House {

    @NonNull
    private final String title;
    @NonNull
    private final String address;
    private final double distanceKm;
    private final boolean verified;
    @DrawableRes
    private final int photoRes;

    public House(@NonNull String title,
                 @NonNull String address,
                 double distanceKm,
                 boolean verified,
                 @DrawableRes int photoRes) {
        this.title = title;
        this.address = address;
        this.distanceKm = distanceKm;
        this.verified = verified;
        this.photoRes = photoRes;
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
}
