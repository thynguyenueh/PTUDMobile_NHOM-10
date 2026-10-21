package com.example.studenthousing.domain;

import com.example.studenthousing.data.model.AccommodationPost;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class FilterCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EXTRA = "extra_filter_criteria";

    public static final String TYPE_APARTMENT = "APARTMENT";
    public static final String TYPE_WHOLE_HOUSE = "WHOLE_HOUSE";
    public static final String TYPE_SHARED_ROOM = "SHARED_ROOM";

    public String province;
    public ArrayList<String> wards = new ArrayList<>();
    public long minPrice = 0L;
    public long maxPrice = Long.MAX_VALUE;
    public String housingType;
    public boolean verifiedOnly;

    public boolean isActive() {
        return province != null
                || !wards.isEmpty()
                || minPrice > 0L
                || maxPrice != Long.MAX_VALUE
                || housingType != null
                || verifiedOnly;
    }

    public boolean matches(AccommodationPost p) {
        if (p == null) return false;

        if (verifiedOnly && !p.isPoliceVerified()) return false;

        double rawPrice = p.getPrice();
        double priceInVnd = rawPrice > 1000 ? rawPrice : rawPrice * 1_000_000.0;
        if (priceInVnd < minPrice || priceInVnd > maxPrice) return false;

        if (province != null && !normalize(province).equals(normalize(p.getCityName()))) {
            return false;
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

        return housingType == null || housingType.equalsIgnoreCase(p.getHouseType());
    }

    static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
