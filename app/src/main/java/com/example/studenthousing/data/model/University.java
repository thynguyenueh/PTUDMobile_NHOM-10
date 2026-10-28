package com.example.studenthousing.data.model;

/**
 * Model class representing a University campus or branch.
 */
public class University {
    private String code;
    private String name;
    private String campus;
    private double latitude;
    private double longitude;
    private boolean verified;

    public University(String code, String name, String campus, double latitude, double longitude, boolean verified) {
        this.code = code;
        this.name = name;
        this.campus = campus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verified = verified;
    }

    public String getDisplayName() {
        if (campus == null || campus.isEmpty()) return name;
        return name + " - " + campus;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
