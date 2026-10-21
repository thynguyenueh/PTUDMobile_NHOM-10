package com.example.studenthousing.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DistanceCalculator {

    private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private static final String ORS_API_KEY = "YOUR_ORS_API_KEY_HERE";
    private static final String USER_AGENT = "StudentHousingApp/1.0 (contact@studenthousing.com)";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface DistanceResultCallback {
        void onSuccess(double distanceKm, String source, boolean isEstimate);
        void onError(Exception e);
    }

    public DistanceCalculator() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void calculateDistance(double lat1, double lon1, double lat2, double lon2, DistanceResultCallback callback) {
        fetchOsrm(lat1, lon1, lat2, lon2, new DistanceResultCallback() {
            @Override
            public void onSuccess(double distanceKm, String source, boolean isEstimate) {
                notifySuccess(callback, distanceKm, source, isEstimate);
            }

            @Override
            public void onError(Exception e) {
                fetchOrs(lat1, lon1, lat2, lon2, new DistanceResultCallback() {
                    @Override
                    public void onSuccess(double distanceKm, String source, boolean isEstimate) {
                        notifySuccess(callback, distanceKm, source, isEstimate);
                    }

                    @Override
                    public void onError(Exception e2) {
                        double estimatedKm = calculateHaversineKm(lat1, lon1, lat2, lon2) * 1.35;
                        double roundedKm = Math.round(estimatedKm * 10.0) / 10.0;
                        notifySuccess(callback, roundedKm, "haversine", true);
                    }
                });
            }
        });
    }

    private void fetchOsrm(double lat1, double lon1, double lat2, double lon2, DistanceResultCallback callback) {
        String url = OSRM_URL + lon1 + "," + lat1 + ";" + lon2 + "," + lat2 + "?overview=false";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        callback.onError(new IOException("OSRM HTTP Error: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(body.string());
                    JSONArray routes = json.optJSONArray("routes");
                    if (routes == null || routes.length() == 0) {
                        callback.onError(new Exception("No OSRM routes found"));
                        return;
                    }
                    double distanceMeters = routes.getJSONObject(0).getDouble("distance");
                    double distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
                    callback.onSuccess(distanceKm, "osrm", false);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    private void fetchOrs(double lat1, double lon1, double lat2, double lon2, DistanceResultCallback callback) {
        if (ORS_API_KEY == null || ORS_API_KEY.contains("YOUR_ORS_API_KEY")) {
            callback.onError(new Exception("ORS API Key not configured"));
            return;
        }

        String url = ORS_URL + "?api_key=" + ORS_API_KEY + "&start=" + lon1 + "," + lat1 + "&end=" + lon2 + "," + lat2;
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        callback.onError(new IOException("ORS HTTP Error: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(body.string());
                    JSONArray features = json.optJSONArray("features");
                    if (features == null || features.length() == 0) {
                        callback.onError(new Exception("No ORS routes found"));
                        return;
                    }
                    JSONObject summary = features.getJSONObject(0)
                            .getJSONObject("properties")
                            .getJSONObject("summary");
                    double distanceMeters = summary.getDouble("distance");
                    double distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
                    callback.onSuccess(distanceKm, "ors", false);
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
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static int calculateMotorbikeMinutes(double distanceKm) {
        if (distanceKm <= 0) return 1;
        return (int) Math.max(1, Math.round((distanceKm / 30.0) * 60));
    }

    public static String formatDistanceText(double distanceKm, boolean isEstimate) {
        String formattedDist = String.format(Locale.US, "%.1f km", distanceKm);
        if (isEstimate) {
            return "~ " + formattedDist + " (Ưước lượng)";
        }
        return formattedDist;
    }

    public static String formatDistanceAndDuration(double distanceKm, boolean isEstimate) {
        String distText = formatDistanceText(distanceKm, isEstimate);
        int minutes = calculateMotorbikeMinutes(distanceKm);
        return distText + " · " + minutes + " phút đi xe máy";
    }

    private void notifySuccess(DistanceResultCallback callback, double distanceKm, String source, boolean isEstimate) {
        mainHandler.post(() -> callback.onSuccess(distanceKm, source, isEstimate));
    }
}
