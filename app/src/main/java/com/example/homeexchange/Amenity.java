package com.example.homeexchange;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Một tiện nghi hiển thị trong màn chi tiết nhà. */
public final class Amenity {

    @DrawableRes
    private final int iconRes;
    @StringRes
    private final int labelRes;

    public Amenity(@DrawableRes int iconRes, @StringRes int labelRes) {
        this.iconRes = iconRes;
        this.labelRes = labelRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }
}
