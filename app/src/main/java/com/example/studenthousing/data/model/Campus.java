package com.example.studenthousing.data.model;

import java.io.Serializable;

public class Campus implements Serializable {
    private String campusCode;
    private String name;
    private String fullName;
    private String type;
    private boolean isSatellite;
    private String category;
    private String district;
    private double lat;
    private double lon;
    private long osmId;
    private String osmType;

    public Campus() {}

    public String getCampusCode() {
        return campusCode;
    }

    public void setCampusCode(String campusCode) {
        this.campusCode = campusCode;
    }

    public String getCode() {
        return campusCode;
    }

    public void setCode(String code) {
        this.campusCode = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSatellite() {
        return isSatellite;
    }

    public void setSatellite(boolean satellite) {
        isSatellite = satellite;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public boolean isUniversity() {
        return "university".equalsIgnoreCase(type);
    }

    public boolean isCollege() {
        return "college".equalsIgnoreCase(type);
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty() && !fullName.equals(name)) {
            return fullName;
        }
        return name != null ? name : "";
    }
}
