package com.example.studenthousing.util;

public final class LocationData {

    private LocationData() {}

    public static final String[] PROVINCES = {
            "TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Bình Dương", "Đồng Nai", "Bà Rịa - Vũng Tàu", "Khánh Hòa", "Thừa Thiên Huế",
            "Lâm Đồng", "Đắc Lắc", "Gia Lai", "An Giang", "Kiên Giang",
            "Nghệ An", "Thanh Hóa", "Quảng Ninh", "Bắc Ninh", "Thái Nguyên"
    };

    public static final String[] WARDS = {
            "Phường Thảo Điền", "Phường An Phú", "Phường Bến Nghé", "Phường Bến Thành",
            "Phường Đa Kao", "Phường Tân Phong", "Phường Tân Phú", "Phường Linh Trung",
            "Phường Linh Xuân", "Phường Linh Chiểu", "Phường Dĩ An", "Phường Lái Thiêu",
            "Phường Cầu Ông Lãnh", "Phường Cô Giang", "Phường Phước Long A",
            "Phường Phước Long B", "Phường Hiệp Phú", "Phường Long Thạnh Mỹ"
    };

    public static String clean(String selected) {
        if (selected == null) return "";
        String s = selected.trim();
        return s.startsWith("Chọn") ? "" : s;
    }

    public static String buildAddress(String houseNumber, String street, String ward, String city) {
        String line1 = ((houseNumber == null ? "" : houseNumber.trim()) + " "
                + (street == null ? "" : street.trim())).trim();
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{line1, ward, city}) {
            if (part == null || part.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
        return sb.toString();
    }
}
