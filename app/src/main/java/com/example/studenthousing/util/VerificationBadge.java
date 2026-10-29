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

    public static String addressBadgeText(String addressValidationStatus) {
        if (Constants.ADDR_VALID.equalsIgnoreCase(addressValidationStatus)) {
            return "ĐÃ KIỂM TRA ĐỊA CHỈ";
        }
        if (Constants.ADDR_INVALID.equalsIgnoreCase(addressValidationStatus)) {
            return "ĐỊA CHỈ KHÔNG TỒN TẠI";
        }
        return "CHƯA KIỂM TRA";
    }
}
