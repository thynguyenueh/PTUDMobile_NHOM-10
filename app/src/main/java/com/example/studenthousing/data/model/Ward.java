package com.example.studenthousing.data.model;

import java.io.Serializable;

/**
 * Model phường/xã TP.HCM. Đọc từ assets/wards.json.
 */
public class Ward implements Serializable {

    private int code;
    private String name;
    private String divisionType;
    private String codename;

    public Ward() {}

    public Ward(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public Ward(int code, String name, String divisionType, String codename) {
        this.code = code;
        this.name = name;
        this.divisionType = divisionType;
        this.codename = codename;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDivisionType() {
        return divisionType;
    }

    public void setDivisionType(String divisionType) {
        this.divisionType = divisionType;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ward)) return false;
        Ward ward = (Ward) o;
        return code == ward.code;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(code);
    }
}
