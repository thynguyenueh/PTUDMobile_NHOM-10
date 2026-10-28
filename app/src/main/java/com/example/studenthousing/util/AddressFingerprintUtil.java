package com.example.studenthousing.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class AddressFingerprintUtil {

    public static String build(int cityCode, int wardCode, String street, String houseNumber) {
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


    private static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(base.hashCode());
        }
    }
}
