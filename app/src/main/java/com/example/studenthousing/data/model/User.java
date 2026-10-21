package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class User {
    @DocumentId
    private String userId;
    private List<String> authProviders;
    private String familyName;
    private String email;
    private String phone;
    private String password;
    private boolean phoneVerified;
    private String identityType = Constants.IDENTITY_NONE;
    private boolean identityVerified;
    private long identityVerifiedAt;
    private String studentId;
    private String universityCode;
    private String universityName;
    private String citizenIdHash;
    private boolean residenceVerified;
    private long residenceVerifiedAt;
    private boolean fullyVerified;
    private String permanentAddress;
    private int permanentCityCode;
    private int permanentWardCode;
    private String permanentCityName;
    private String permanentWardName;
    private boolean isVerified;
    private String avatarUrl;
    private long createdAt;
    private long updatedAt;

    public User() {}

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }



    public String getIdentityType() { return identityType; }
    public void setIdentityType(String identityType) { this.identityType = identityType; }

    public boolean isIdentityVerified() { return identityVerified; }
    public void setIdentityVerified(boolean identityVerified) { this.identityVerified = identityVerified; }

    public long getIdentityVerifiedAt() { return identityVerifiedAt; }
    public void setIdentityVerifiedAt(long identityVerifiedAt) { this.identityVerifiedAt = identityVerifiedAt; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getUniversityCode() { return universityCode; }
    public void setUniversityCode(String universityCode) { this.universityCode = universityCode; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public String getCitizenIdHash() { return citizenIdHash; }
    public void setCitizenIdHash(String citizenIdHash) { this.citizenIdHash = citizenIdHash; }

    public boolean isResidenceVerified() { return residenceVerified; }
    public void setResidenceVerified(boolean residenceVerified) { this.residenceVerified = residenceVerified; }

    public long getResidenceVerifiedAt() { return residenceVerifiedAt; }
    public void setResidenceVerifiedAt(long residenceVerifiedAt) { this.residenceVerifiedAt = residenceVerifiedAt; }

    public boolean isFullyVerified() { return fullyVerified; }
    public void setFullyVerified(boolean fullyVerified) { this.fullyVerified = fullyVerified; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public int getPermanentCityCode() { return permanentCityCode; }
    public void setPermanentCityCode(int permanentCityCode) { this.permanentCityCode = permanentCityCode; }

    public int getPermanentWardCode() { return permanentWardCode; }
    public void setPermanentWardCode(int permanentWardCode) { this.permanentWardCode = permanentWardCode; }

    public String getPermanentCityName() { return permanentCityName; }
    public void setPermanentCityName(String permanentCityName) { this.permanentCityName = permanentCityName; }

    public String getPermanentWardName() { return permanentWardName; }
    public void setPermanentWardName(String permanentWardName) { this.permanentWardName = permanentWardName; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
