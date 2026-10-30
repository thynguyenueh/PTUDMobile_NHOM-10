package com.example.studenthousing.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class UtilityLoader {

    private UtilityLoader() {}

    public static List<String> loadUtilities(Context context) {
        List<String> list = new ArrayList<>();
        if (context == null) return list;
        try (InputStream is = context.getAssets().open("utilities.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String label = obj.optString("label", "");
                if (!label.isEmpty()) {
                    list.add(label);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
