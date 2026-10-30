package com.example.studenthousing.util;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class LocationService {
    private static final String USER_AGENT = "StudentHousingApp/1.0 (contact@studenthousing.com)";
    /** Nominatim yêu cầu tối thiểu 1s giữa 2 request. */
    private static final long MIN_REQUEST_INTERVAL_MS = 1000L;

    // Executor cố định 4 thread: mỗi request chiếm 1 thread,
    // semaphore chỉ chặn thread đó, không chặn cả executor.
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    // Semaphore(1) đảm bảo chỉ 1 request được "vào cửa" tại 1 thời điểm.
    private static final Semaphore RATE_LIMIT = new Semaphore(1, true);
    private static final AtomicLong LAST_REQUEST_TIME = new AtomicLong(0L);

    private LocationService() {}

    public static class GeoResult {
        public final Location location;
        public final String formattedAddress;

        public GeoResult(Location location, String formattedAddress) {
            this.location = location;
            this.formattedAddress = formattedAddress;
        }
    }

    public static void getCoordinatesFromAddress(Context context, String address, String wardName,
                                                 AppCallback<GeoResult> callback) {
        if (context == null || callback == null) return;
        String query = address == null ? "" : address.trim();
        if (wardName != null && !wardName.trim().isEmpty()
                && !query.toLowerCase(Locale.ROOT)
                .contains(wardName.trim().toLowerCase(Locale.ROOT))) {
            query = query.isEmpty() ? wardName.trim() : query + ", " + wardName.trim();
        }
        if (query.isEmpty()) {
            callback.onError(new IllegalArgumentException("Address is required"));
            return;
        }

        final String addressQuery = query;
        EXECUTOR.execute(() -> {
            try {
                // Rate-limit: chỉ thread hiện tại bị block, không block executor.
                RATE_LIMIT.acquire();
                try {
                    long now = System.currentTimeMillis();
                    long diff = now - LAST_REQUEST_TIME.get();
                    if (diff < MIN_REQUEST_INTERVAL_MS) {
                        long sleep = MIN_REQUEST_INTERVAL_MS - diff;
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            MAIN.post(() -> callback.onError(ie));
                            return;
                        }
                    }
                    LAST_REQUEST_TIME.set(System.currentTimeMillis());
                } finally {
                    RATE_LIMIT.release();
                }

                String encoded = URLEncoder.encode(
                        addressQuery + ", TP. Hồ Chí Minh, Việt Nam",
                        StandardCharsets.UTF_8.name());
                String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=jsonv2&addressdetails=1&limit=1&countrycodes=vn";

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .build();

                try (Response response = CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        final int code = response.code();
                        MAIN.post(() -> callback.onError(
                                new IOException("Nominatim HTTP: " + code)));
                        return;
                    }
                    JSONArray arr = new JSONArray(response.body().string());
                    if (arr.length() == 0) {
                        MAIN.post(() -> callback.onError(
                                new IOException("Không tìm thấy tọa độ cho địa chỉ")));
                        return;
                    }
                    JSONObject obj = arr.getJSONObject(0);
                    if (!obj.has("lat") || !obj.has("lon")) {
                        MAIN.post(() -> callback.onError(
                                new IOException("Nominatim thiếu lat/lon")));
                        return;
                    }

                    // Kiểm tra chi tiết địa chỉ trả về từ Nominatim để lọc địa chỉ ảo / chung chung
                    if (obj.has("address")) {
                        JSONObject addrObj = obj.getJSONObject("address");
                        boolean hasStreetOrArea = addrObj.has("road") || addrObj.has("street") ||
                                addrObj.has("suburb") || addrObj.has("neighbourhood") ||
                                addrObj.has("quarter") || addrObj.has("city_district");
                        if (!hasStreetOrArea) {
                            MAIN.post(() -> callback.onError(
                                    new IOException("Địa chỉ không đủ chi tiết hoặc không tồn tại tên đường/phường")));
                            return;
                        }
                    }

                    Location location = new Location("nominatim");
                    location.setLatitude(obj.getDouble("lat"));
                    location.setLongitude(obj.getDouble("lon"));

                    String placeName = obj.optString("name", "").trim();
                    String displayName = obj.optString("display_name", addressQuery);
                    if (!placeName.isEmpty() && !displayName.toLowerCase(Locale.ROOT).contains(placeName.toLowerCase(Locale.ROOT))) {
                        displayName = placeName + ", " + displayName;
                    }
                    if (displayName.endsWith(", Việt Nam")) {
                        displayName = displayName.substring(0, displayName.length() - 10).trim();
                    }

                    GeoResult result = new GeoResult(location, displayName);
                    MAIN.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                MAIN.post(() -> callback.onError(e));
            }
        });
    }
}
