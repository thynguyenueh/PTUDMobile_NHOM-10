package com.example.studenthousing.data.loader;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.model.Campus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CampusDataLoader {
    private static final String TAG = "CampusDataLoader";
    private static List<Campus> cachedCampuses = null;

    public static synchronized List<Campus> loadCampuses(Context context) {
        if (cachedCampuses != null) return cachedCampuses;
        cachedCampuses = new ArrayList<>();
        if (context == null) return cachedCampuses;

        try (InputStream is = context.getAssets().open("campuses.json")) {
            byte[] buffer = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Campus c = new Campus();
                c.setCampusCode(obj.optString("campusCode"));
                c.setName(obj.optString("name"));
                c.setFullName(obj.optString("fullName"));
                c.setDistrict(obj.optString("district"));
                c.setLat(obj.optDouble("lat", 0.0));
                c.setLon(obj.optDouble("lon", 0.0));
                // Bổ sung các field trước đây bị bỏ sót:
                c.setType(obj.optString("type"));
                c.setSatellite(obj.optBoolean("isSatellite", false));
                c.setCategory(obj.optString("category"));
                c.setOsmId(obj.optLong("osmId", 0L));
                c.setOsmType(obj.optString("osmType"));
                cachedCampuses.add(c);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đọc file campuses.json", e);
        }
        return cachedCampuses;
    }

    public static Campus getByCode(Context context, String code) {
        if (code == null) return null;
        for (Campus c : loadCampuses(context)) {
            if (code.equalsIgnoreCase(c.getCampusCode()) || code.equalsIgnoreCase(c.getCode())) {
                return c;
            }
        }
        return null;
    }

    /** Xoá cache — hữu ích cho unit test hoặc khi hot-reload asset. */
    public static synchronized void invalidateCache() {
        cachedCampuses = null;
    }
}