package com.example.studenthousing.data.loader;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.model.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UtilitiesDataLoader {
    private static final String TAG = "UtilitiesDataLoader";
    private static List<Utility> cachedUtilities = null;

    public static synchronized List<Utility> loadUtilities(Context context) {
        if (cachedUtilities != null) {
            return cachedUtilities;
        }
        cachedUtilities = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("utilities.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Utility u = new Utility(
                        obj.optString("code"),
                        obj.optString("label"),
                        obj.optString("icon")
                );
                cachedUtilities.add(u);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đọc file utilities.json", e);
        }
        return cachedUtilities;
    }

    public static Utility getByCode(Context context, String code) {
        if (code == null) return null;
        for (Utility u : loadUtilities(context)) {
            if (code.equalsIgnoreCase(u.getCode())) return u;
        }
        return null;
    }

    public static String getLabelOrFallback(Context context, String codeOrLabel) {
        if (codeOrLabel == null) return "";
        Utility u = getByCode(context, codeOrLabel);
        if (u != null && u.getLabel() != null && !u.getLabel().isEmpty()) {
            return u.getLabel();
        }
        return codeOrLabel;
    }
}
