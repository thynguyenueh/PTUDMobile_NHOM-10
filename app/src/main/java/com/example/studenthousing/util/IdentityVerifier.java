package com.example.studenthousing.util;

import com.example.studenthousing.data.model.VerifiedAddress;

public class IdentityVerifier {

    public static boolean isValidStudent(String studentId, String universityCode, String email) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        if (universityCode == null || universityCode.trim().isEmpty()) {
            return false;
        }
        if (email != null && !email.trim().isEmpty()) {
            String lower = email.trim().toLowerCase();
            if (lower.contains("@") && !lower.endsWith(".edu.vn")) {
            }
        }
        return true;
    }

    public static boolean isValidCitizenId(String citizenId) {
        if (citizenId == null) {
            return false;
        }
        String trimmed = citizenId.trim();
        return trimmed.matches("^\\d{12}$");
    }

    public static boolean isValidResidence(VerifiedAddress addr) {
        if (addr == null) {
            return false;
        }
        return Constants.ADDR_VERIFIED.equalsIgnoreCase(addr.getStatus());
    }
}
