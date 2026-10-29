package com.example.studenthousing.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.studenthousing.data.model.Campus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DistanceCalculator {

    private static final String OSRM_ROUTE_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final String OSRM_TABLE_URL = "https://router.project-osrm.org/table/v1/driving/";
    private static final String USER_AGENT = "StudentHousingApp/1.0 (contact@studenthousing.com)";
    private static final double ROAD_DISTANCE_FACTOR = 1.35;
    // SRS: 128–129 campus. OSRM Table hỗ trợ tối đa 100 destination/request.
    // Giải pháp: pre-filter bằng Haversine, chỉ gửi top-100 gần nhất cho OSRM.
    private static final int MAX_OSRM_DESTINATIONS = 100;
    private static final double PRE_FILTER_RADIUS_KM = 15.0;   // rộng hơn 10km để bù sai số

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface DistanceResultCallback {
        void onSuccess(double distanceKm, String source, boolean isEstimate);
        void onError(Exception e);
    }

    public interface TableResultCallback {
        void onSuccess(double[] distancesMeters);
        void onError(Exception e);
    }

    public DistanceCalculator() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void calculateDistance(double lat1, double lon1, double lat2, double lon2,
                                  DistanceResultCallback callback) {
        fetchOsrm(lat1, lon1, lat2, lon2, new DistanceResultCallback() {
            @Override public void onSuccess(double d, String s, boolean e) { notifySuccess(callback, d, s, e); }
            @Override public void onError(Exception e) { notifyError(callback, e); }
        });
    }

    /**
     * OSRM Table — tự pre-filter nếu destinations > 100.
     * Pre-filter bằng Haversine * 1.35, lấy top-100 gần nhất.
     * Fallback Haversine cho các campus còn lại.
     */
    public void fetchOsrmTable(double originLat, double originLon, List<Campus> destinations,
                               TableResultCallback callback) {
        if (destinations == null || destinations.isEmpty()) {
            notifyTableSuccess(callback, new double[0]);
            return;
        }

        // Bước 1: nếu > 100, pre-filter
        List<Campus> toRequest = destinations;
        if (destinations.size() > MAX_OSRM_DESTINATIONS) {
            toRequest = preFilterByHaversine(originLat, originLon, destinations, MAX_OSRM_DESTINATIONS);
        }

        // Bước 2: build URL
        StringBuilder urlBuilder = new StringBuilder(OSRM_TABLE_URL);
        urlBuilder.append(originLon).append(",").append(originLat);
        for (Campus u : toRequest) {
            urlBuilder.append(";").append(u.getLon()).append(",").append(u.getLat());
        }
        urlBuilder.append("?sources=0&annotations=distance");

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", USER_AGENT)
                .build();

        final List<Campus> requested = toRequest;
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                notifyTableError(callback, e);
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        notifyTableError(callback, new IOException("OSRM Table HTTP Error: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(body.string());
                    if (!"Ok".equalsIgnoreCase(json.optString("code"))) {
                        notifyTableError(callback, new Exception("OSRM Table error code: " + json.optString("code")));
                        return;
                    }
                    JSONArray matrix = json.optJSONArray("distances");
                    if (matrix == null || matrix.length() == 0) {
                        notifyTableError(callback, new Exception("No distance matrix"));
                        return;
                    }
                    JSONArray row = matrix.getJSONArray(0);
                    // Build map code -> meters; campus không có trong OSRM → -1
                    java.util.Map<String, Double> osrmResult = new java.util.HashMap<>();
                    for (int i = 0; i < requested.size(); i++) {
                        Campus u = requested.get(i);
                        double m = (i + 1 < row.length() && !row.isNull(i + 1)) ? row.getDouble(i + 1) : -1.0;
                        osrmResult.put(u.getCode(), m);
                    }
                    // Trả về mảng đúng thứ tự `destinations` gốc
                    double[] result = new double[destinations.size()];
                    for (int i = 0; i < destinations.size(); i++) {
                        Double m = osrmResult.get(destinations.get(i).getCode());
                        result[i] = (m != null) ? m : -1.0;
                    }
                    notifyTableSuccess(callback, result);
                } catch (Exception e) {
                    notifyTableError(callback, e);
                }
            }
        });
    }

    private List<Campus> preFilterByHaversine(double lat, double lon, List<Campus> all, int maxCount) {
        java.util.List<Campus> filtered = new java.util.ArrayList<>();
        for (Campus c : all) {
            double km = calculateHaversineKm(lat, lon, c.getLat(), c.getLon()) * ROAD_DISTANCE_FACTOR;
            if (km <= PRE_FILTER_RADIUS_KM) filtered.add(c);
        }
        filtered.sort(java.util.Comparator.comparingDouble(c ->
                calculateHaversineKm(lat, lon, c.getLat(), c.getLon())));
        if (filtered.size() > maxCount) {
            return new java.util.ArrayList<>(filtered.subList(0, maxCount));
        }
        return filtered;
    }

    private void fetchOsrm(double lat1, double lon1, double lat2, double lon2,
                           DistanceResultCallback callback) {
        String url = OSRM_ROUTE_URL + lon1 + "," + lat1 + ";" + lon2 + "," + lat2 + "?overview=false";
        Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        callback.onError(new IOException("OSRM HTTP Error: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(body.string());
                    JSONArray routes = json.optJSONArray("routes");
                    if (routes == null || routes.length() == 0) {
                        callback.onError(new Exception("No OSRM routes"));
                        return;
                    }
                    double meters = routes.getJSONObject(0).getDouble("distance");
                    callback.onSuccess(roundKm(meters / 1000.0), "OSRM", false);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static String formatDistanceText(double distanceKm, boolean isEstimate) {
        String formatted = String.format(Locale.US, "%.1f km", distanceKm);
        return isEstimate ? "~ " + formatted + " (Ước lượng)" : formatted;
    }

    private static double roundKm(double km) { return Math.round(km * 10.0) / 10.0; }

    private void notifySuccess(DistanceResultCallback cb, double km, String source, boolean est) {
        mainHandler.post(() -> cb.onSuccess(km, source, est));
    }
    private void notifyError(DistanceResultCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
    private void notifyTableSuccess(TableResultCallback cb, double[] d) {
        mainHandler.post(() -> cb.onSuccess(d));
    }
    private void notifyTableError(TableResultCallback cb, Exception e) {
        mainHandler.post(() -> cb.onError(e));
    }
}