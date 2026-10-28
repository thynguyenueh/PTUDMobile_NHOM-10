package com.example.studenthousing.data.model.province;

import java.util.ArrayList;
import java.util.List;

public class Province {
    private int code;
    private String name;
    private String divisionType;
    private String codename;
    private int phoneCode;
    private List<Ward> wards = new ArrayList<>();

    public Province() {}

    public Province(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public Province(int code, String name, String divisionType, String codename, int phoneCode) {
        this.code = code;
        this.name = name;
        this.divisionType = divisionType;
        this.codename = codename;
        this.phoneCode = phoneCode;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDivisionType() { return divisionType; }
    public void setDivisionType(String divisionType) { this.divisionType = divisionType; }

    public String getCodename() { return codename; }
    public void setCodename(String codename) { this.codename = codename; }

    public int getPhoneCode() { return phoneCode; }
    public void setPhoneCode(int phoneCode) { this.phoneCode = phoneCode; }

    public List<Ward> getWards() { return wards; }
    public void setWards(List<Ward> wards) { this.wards = wards; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
