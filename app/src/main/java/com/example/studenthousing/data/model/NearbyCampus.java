package com.example.studenthousing.data.model;

import java.io.Serializable;

public class NearbyCampus implements Serializable {
    private static final long serialVersionUID = 1L;

    private String campusId;
    private double km;
    private boolean estimate;

    public NearbyCampus() {}

    public NearbyCampus(String campusId, double km, boolean estimate) {
        this.campusId = campusId;
        this.km = km;
        this.estimate = estimate;
    }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public double getKm() { return km; }
    public void setKm(double km) { this.km = km; }

    public boolean isEstimate() { return estimate; }
    public void setEstimate(boolean estimate) { this.estimate = estimate; }
}
