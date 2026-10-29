package com.example.studenthousing.data.model;

import java.io.Serializable;

public class Utility implements Serializable {
    private String code;
    private String label;
    private String icon;

    public Utility() {}

    public Utility(String code, String label, String icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
