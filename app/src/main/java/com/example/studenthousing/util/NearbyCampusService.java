package com.example.studenthousing.util;

import android.content.Context;
import android.util.Log;

import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.repository.CampusRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NearbyCampusService {
    private static final String TAG = "NearbyCampusService";

    /** Ngưỡng lọc bằng Haversine trước khi gọi OSRM (km).
     *  Chỉ dùng để giảm số destination gửi OSRM — KHÔNG dùng để hiển thị. */
    private static final double PRE_FILTER_KM = 7.0;

    /** Ngưỡng km tối đa để nhận vào kết quả cuối (dựa trên km OSRM thật). */
    private static final double MAX_RESULT_KM = 5.0;

    /** Hệ số quy đổi Haversine → đường bộ, chỉ dùng cho pre-filter. */
    private static final double ROAD_FACTOR = 1.35;

    public interface NearbyCallback {
        void onResult(List<NearbyCampus> campuses);
    }

    /**
     * Tính nhanh bằng Haversine — KHÔNG gọi mạng, có estimate.
     * Đây là API riêng, caller chủ động chọn dùng khi muốn ước lượng offline.
     */
    public static List<NearbyCampus> calculateNearby(Context context, double lat, double lon) {
        List<NearbyCampus> result = new ArrayList<>();
        if ((lat == 0.0 && lon == 0.0) || context == null) return result;

        List<Campus> campuses = CampusRepository.getInstance(context).getAllCampuses();
        if (campuses == null) return result;

        for (Campus c : campuses) {
            if (c == null || (c.getLat() == 0.0 && c.getLon() == 0.0)) continue;
            double km = DistanceCalculator.calculateHaversineKm(lat, lon, c.getLat(), c.getLon())
                    * ROAD_FACTOR;
            result.add(new NearbyCampus(c.getCode(), round1(km), true));
        }
        result.sort(Comparator.comparingDouble(NearbyCampus::getKm));
        return result;
    }

    /**
     * Tính khoảng cách bằng OSRM Table — CHỈ trả kết quả thật, KHÔNG fallback.
     *
     * Luồng:
     *  1. Haversine pre-filter (≤ 7km) → giảm số destination gửi OSRM.
     *  2. Gọi OSRM Table 1 lần.
     *  3. Campus nào OSRM trả -1 → BỎ (không ước lượng).
     *  4. Campus nào km OSRM > 5km → BỎ.
     *  5. Sort tăng dần theo km.
     *
     * Nếu OSRM lỗi toàn bộ → trả list rỗng (callback onResult(emptyList)).
     */
    public static void calculateNearbyWithRouting(Context context, double lat, double lon,
                                                  NearbyCallback callback) {
        if (callback == null) return;
        if ((lat == 0.0 && lon == 0.0) || context == null) {
            callback.onResult(Collections.emptyList());
            return;
        }

        List<Campus> all = CampusRepository.getInstance(context).getAllCampuses();
        if (all == null || all.isEmpty()) {
            callback.onResult(Collections.emptyList());
            return;
        }

        // Bước 1: Haversine pre-filter — chỉ để giảm số destination gửi OSRM
        List<Campus> candidates = new ArrayList<>();
        for (Campus c : all) {
            if (c == null || (c.getLat() == 0.0 && c.getLon() == 0.0)) continue;
            double estKm = DistanceCalculator.calculateHaversineKm(lat, lon, c.getLat(), c.getLon())
                    * ROAD_FACTOR;
            if (estKm <= PRE_FILTER_KM) {
                candidates.add(c);
            }
        }

        if (candidates.isEmpty()) {
            callback.onResult(Collections.emptyList());
            return;
        }

        // Bước 2: OSRM Table
        DistanceCalculator calc = new DistanceCalculator();
        calc.fetchOsrmTable(lat, lon, candidates,
                new DistanceCalculator.TableResultCallback() {
                    @Override
                    public void onSuccess(double[] distancesMeters) {
                        // Bước 3 + 4: chỉ nhận kết quả thật, bỏ campus không có route
                        List<NearbyCampus> results = new ArrayList<>();
                        for (int i = 0; i < candidates.size(); i++) {
                            Campus c = candidates.get(i);
                            double meters = (i < distancesMeters.length) ? distancesMeters[i] : -1.0;

                            // Không có route → bỏ, KHÔNG ước lượng
                            if (meters <= 0) {
                                Log.d(TAG, "Bỏ campus " + c.getCode()
                                        + " — OSRM không trả km");
                                continue;
                            }

                            double km = round1(meters / 1000.0);
                            if (km > MAX_RESULT_KM) continue;

                            results.add(new NearbyCampus(c.getCode(), km, false));
                        }
                        results.sort(Comparator.comparingDouble(NearbyCampus::getKm));
                        callback.onResult(results);
                    }

                    @Override
                    public void onError(Exception e) {
                        // OSRM lỗi toàn bộ → trả rỗng, không ước lượng
                        Log.w(TAG, "OSRM Table lỗi, trả kết quả rỗng: " + e.getMessage());
                        callback.onResult(Collections.emptyList());
                    }
                });
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}