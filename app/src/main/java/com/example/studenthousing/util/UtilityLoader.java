package com.example.studenthousing.util;

import android.content.Context;

import com.example.studenthousing.data.loader.UtilitiesDataLoader;
import com.example.studenthousing.data.model.Utility;

import java.util.ArrayList;
import java.util.List;

public final class UtilityLoader {

    private UtilityLoader() {}

    public static List<String> loadUtilities(Context context) {
        List<String> list = new ArrayList<>();
        if (context == null) return list;
        List<Utility> utilities = UtilitiesDataLoader.loadUtilities(context);
        for (Utility u : utilities) {
            if (u.getLabel() != null && !u.getLabel().isEmpty()) {
                list.add(u.getLabel());
            }
        }
        return list;
    }
}
