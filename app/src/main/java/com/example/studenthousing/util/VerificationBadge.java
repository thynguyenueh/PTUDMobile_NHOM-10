package com.example.studenthousing.util;

public class VerificationBadge {

    public static String badgeText(boolean identityVerified, boolean residenceVerified) {
        if (identityVerified && residenceVerified) {
            return "ĐÃ XÁC THỰC";
        }
        if (identityVerified) {
            return "ĐÃ XÁC THỰC DANH TÍNH";
        }
        if (residenceVerified) {
            return "ĐÃ XÁC THỰC CƯ TRÚ";
        }
        return "CHƯA XÁC THỰC";
    }

    public static String addressBadgeText(String addressVerificationStatus) {
        if (Constants.ADDR_VERIFIED.equalsIgnoreCase(addressVerificationStatus)) {
            return "ĐÃ XÁC THỰC CƯ TRÚ";
        }
        if (Constants.ADDR_DISPUTED.equalsIgnoreCase(addressVerificationStatus)) {
            return "ĐANG TRANH CHẤP";
        }
        return "CHƯA XÁC THỰC ĐỊA CHỈ";
    }
}
