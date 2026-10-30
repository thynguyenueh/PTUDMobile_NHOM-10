package com.example.studenthousing.data.model;

public class Ward {
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

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
