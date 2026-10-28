package com.example.studenthousing.domain;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.util.Constants;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EXTRA = "extra_filter_criteria";

    public String province = Constants.TPHCM_CITY_NAME; // "TP. Hồ Chí Minh"
    public String searchQuery = "";
    public ArrayList<String> wards = new ArrayList<>();
    public String campusId;
    public double maxDistanceKm = 0.0;
    public ArrayList<String> selectedUtilities = new ArrayList<>();

    public boolean isActive() {
        return (searchQuery != null && !searchQuery.trim().isEmpty()) || !wards.isEmpty() || (campusId != null && !campusId.isEmpty()) || maxDistanceKm > 0.0 || !selectedUtilities.isEmpty();
    }

    public boolean matches(AccommodationPost p) {
        if (p == null) return false;

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = normalize(searchQuery);
            boolean titleMatch = normalize(p.getTitle()).contains(q);
            boolean addrMatch = normalize(p.getAddress()).contains(q);
            boolean nameMatch = normalize(p.getFamilyName()).contains(q);
            boolean streetMatch = normalize(p.getStreetName()).contains(q);
            boolean wardMatch = normalize(p.getWardName()).contains(q);
            boolean descMatch = normalize(p.getDescription()).contains(q);
            if (!titleMatch && !addrMatch && !nameMatch && !streetMatch && !wardMatch && !descMatch) {
                return false;
            }
        }

        if (!wards.isEmpty()) {
            String postWard = normalize(p.getWardName());
            boolean found = false;
            for (String w : wards) {
                if (normalize(w).equals(postWard)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        if (campusId != null && !campusId.isEmpty()) {
            if (p.getNearbyCampusIds() == null || !p.getNearbyCampusIds().contains(campusId)) {
                return false;
            }
            if (maxDistanceKm > 0.0 && p.getNearbyCampuses() != null) {
                boolean distanceMatch = false;
                for (NearbyCampus nc : p.getNearbyCampuses()) {
                    if (nc != null && campusId.equals(nc.getCampusId())) {
                        if (nc.getKm() <= maxDistanceKm) {
                            distanceMatch = true;
                            break;
                        }
                    }
                }
                if (!distanceMatch) return false;
            }
        }

        if (selectedUtilities != null && !selectedUtilities.isEmpty()) {
            if (p.getUtilities() == null) return false;
            List<String> postUtils = new ArrayList<>();
            for (String u : p.getUtilities()) {
                postUtils.add(normalize(u));
            }
            for (String reqU : selectedUtilities) {
                if (!postUtils.contains(normalize(reqU))) {
                    return false;
                }
            }
        }

        return true;
    }

    static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
