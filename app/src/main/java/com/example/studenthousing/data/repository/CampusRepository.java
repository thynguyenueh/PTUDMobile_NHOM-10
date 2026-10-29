package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.loader.CampusDataLoader;
import com.example.studenthousing.data.model.Campus;

import java.util.Collections;
import java.util.List;

public class CampusRepository {
    private static volatile CampusRepository INSTANCE;

    private final List<Campus> allCampuses;

    private CampusRepository(Context context) {
        this.allCampuses = CampusDataLoader.loadCampuses(context.getApplicationContext());
    }

    public static CampusRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CampusRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CampusRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public List<Campus> getAllCampuses() {
        return allCampuses != null ? allCampuses : Collections.emptyList();
    }

    public Campus findByCode(String code) {
        if (code == null) return null;
        for (Campus campus : getAllCampuses()) {
            if (campus == null) continue;
            if (code.equalsIgnoreCase(campus.getCampusCode()) || code.equalsIgnoreCase(campus.getCode())) {
                return campus;
            }
        }
        return null;
    }

    public Campus findCampusByCode(String code) {
        return findByCode(code);
    }
}
