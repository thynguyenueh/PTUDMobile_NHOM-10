package com.example.studenthousing.ui.post;

import android.content.Context;

import com.example.studenthousing.R;
import com.example.studenthousing.data.loader.UtilitiesDataLoader;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.repository.CampusRepository;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PostDisplayFormatter {
    private PostDisplayFormatter() {}

    public static String getPublicArea(Context context, AccommodationPost post) {
        String ward = post.getWardName() == null ? "" : post.getWardName().trim();
        String city = post.getCityName() == null ? "" : post.getCityName().trim();
        if (!ward.isEmpty() && !city.isEmpty()) return ward + ", " + city;
        if (!ward.isEmpty()) return ward;
        if (!city.isEmpty()) return city;
        return "Chưa có thông tin khu vực";
    }

    public static int getStatusLabel(AccommodationPost post) {
        if (Constants.POST_EXCHANGED.equalsIgnoreCase(post.getStatus())) {
            return R.string.status_exchanged;
        }
        return 0;
    }

    public static int getCompactStatusLabel(AccommodationPost post) {
        return getStatusLabel(post);
    }

    public static String getNearestCampusSummary(Context context, AccommodationPost post) {
        List<NearbyCampus> campuses = post.getNearbyCampuses();
        if (campuses == null || campuses.isEmpty()) return null;

        NearbyCampus nearest = null;
        for (NearbyCampus campus : campuses) {
            if (campus == null || campus.getCampusId() == null || campus.getKm() <= 0) continue;
            if (nearest == null || campus.getKm() < nearest.getKm()) nearest = campus;
        }
        if (nearest == null) return null;

        Campus campusModel = CampusRepository.getInstance(context).findByCode(nearest.getCampusId());
        String name = campusModel == null ? nearest.getCampusId() : campusModel.getDisplayName();
        String distance = String.format(Locale.getDefault(), "%.1f km", nearest.getKm());
        if (nearest.isEstimate()) distance = "~" + distance;
        return "Cách " + name + ": " + distance;
    }

    public static String getUtilitiesSummary(Context context, AccommodationPost post) {
        List<String> utilities = post.getUtilities();
        if (utilities == null || utilities.isEmpty()) return null;

        List<String> visibleUtilities = new ArrayList<>();
        int utilityCount = 0;
        for (String utility : utilities) {
            if (utility == null || utility.trim().isEmpty()) continue;
            utilityCount++;
            if (visibleUtilities.size() < 2) {
                visibleUtilities.add(UtilitiesDataLoader.getLabelOrFallback(context, utility.trim()));
            }
        }
        if (visibleUtilities.isEmpty()) return null;

        StringBuilder summaryBuilder = new StringBuilder();
        for (String utility : visibleUtilities) {
            if (summaryBuilder.length() > 0) summaryBuilder.append(" · ");
            summaryBuilder.append(utility);
        }
        String summary = summaryBuilder.toString();
        int remaining = utilityCount - visibleUtilities.size();
        return remaining > 0
                ? summary + " (+" + remaining + ")"
                : summary;
    }
}