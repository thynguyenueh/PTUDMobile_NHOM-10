package com.example.studenthousing.data.loader;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.model.Ward;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WardsDataLoader {
    private static final String TAG = "WardsDataLoader";
    private static List<Ward> cachedWards = null;
    private static WardsDataLoader instance;

    public static synchronized WardsDataLoader getInstance() {
        if (instance == null) instance = new WardsDataLoader();
        return instance;
    }

    public static synchronized List<Ward> loadWards(Context context) {
        if (cachedWards != null) return cachedWards;
        cachedWards = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("wards.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Ward w = new Ward(obj.optInt("code"), obj.optString("name"));
                cachedWards.add(w);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đọc file wards.json", e);
        }
        return cachedWards;
    }
    public static Ward findByName(Context context, String name) {
        if (name == null) return null;
        for (Ward w : loadWards(context)) {
            if (name.equalsIgnoreCase(w.getName())) return w;
        }
        return null;
    }
    public List<Ward> loadAll(Context context) {
        return loadWards(context);
    }

    public static Ward getByCode(Context context, int code) {
        for (Ward w : loadWards(context)) {
            if (w.getCode() == code) return w;
        }
        return null;
    }
}
