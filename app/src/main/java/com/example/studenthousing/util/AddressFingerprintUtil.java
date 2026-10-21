package com.example.studenthousing.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class AddressFingerprintUtil {

    public enum MatchLevel {
        EXACT,        // Same fingerprint
        SAME_HOUSE,   // Same house
        SAME_STREET,  // Same street, different house number
        SAME_WARD,    // Same ward, different street
        SAME_CITY,    // Same city, different ward
        DIFFERENT     // Different city
    }

    public static String build(int cityCode, int wardCode, String street, String houseNumber) {
        return build(cityCode, wardCode, street, houseNumber, null);
    }

    public static String build(int cityCode, int wardCode, String street, String houseNumber, String roomNumber) {
        String normStreet = normalize(street);
        String normHouse = normalize(houseNumber);

        String rawKey = cityCode + "|" + wardCode + "|" + normStreet + "|" + normHouse;
        return sha256(rawKey);
    }

    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        s = pattern.matcher(s).replaceAll("");

        s = s.replace('đ', 'd').replace('Đ', 'd');

        s = s.replaceAll("\\b(duong|pho|ngo|hem)\\b", "");

        s = s.replaceAll("[^a-z0-9\\s]", " ");

        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public static MatchLevel compare(int cityCodeA, int wardCodeA, String streetA, String houseA,
                                     int cityCodeB, int wardCodeB, String streetB, String houseB) {
        if (cityCodeA != cityCodeB) {
            return MatchLevel.DIFFERENT;
        }
        if (wardCodeA != wardCodeB) {
            return MatchLevel.SAME_CITY;
        }

        String normStreetA = normalize(streetA);
        String normStreetB = normalize(streetB);
        if (!normStreetA.equals(normStreetB)) {
            return MatchLevel.SAME_WARD;
        }

        String normHouseA = normalize(houseA);
        String normHouseB = normalize(houseB);
        if (!normHouseA.equals(normHouseB)) {
            return MatchLevel.SAME_STREET;
        }

        return MatchLevel.EXACT;
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
