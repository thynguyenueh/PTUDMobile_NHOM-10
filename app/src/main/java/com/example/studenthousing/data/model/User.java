package com.example.studenthousing.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class User {
    @DocumentId
    private String userId;
    private List<String> authProviders;   // ["password"] | ["phone"] | ["google"]
    private String familyName;
    private String email;                  // fake email hoặc email Google
    private String phone;
    private boolean phoneVerified;
    private String role;                   // "STUDENT" | "PARENT"
    private String studentId;
    private String universityCode;
    private String universityName;
    private int permanentCityCode;
    private int permanentDistrictCode;
    private int permanentWardCode;
    private boolean isVerified;
    private boolean residenceVerified;
    private long residenceVerifiedAt;
    private String avatarUrl;
    private long createdAt;
    private long updatedAt;

    public User() {} // Constructor rỗng cho Firestore

    // Getters & Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getAuthProviders() { return authProviders; }
    public void setAuthProviders(List<String> authProviders) { this.authProviders = authProviders; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getUniversityCode() { return universityCode; }
    public void setUniversityCode(String universityCode) { this.universityCode = universityCode; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public int getPermanentCityCode() { return permanentCityCode; }
    public void setPermanentCityCode(int permanentCityCode) { this.permanentCityCode = permanentCityCode; }

    public int getPermanentDistrictCode() { return permanentDistrictCode; }
    public void setPermanentDistrictCode(int permanentDistrictCode) { this.permanentDistrictCode = permanentDistrictCode; }

    public int getPermanentWardCode() { return permanentWardCode; }
    public void setPermanentWardCode(int permanentWardCode) { this.permanentWardCode = permanentWardCode; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isResidenceVerified() { return residenceVerified; }
    public void setResidenceVerified(boolean residenceVerified) { this.residenceVerified = residenceVerified; }

    public long getResidenceVerifiedAt() { return residenceVerifiedAt; }
    public void setResidenceVerifiedAt(long residenceVerifiedAt) { this.residenceVerifiedAt = residenceVerifiedAt; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
