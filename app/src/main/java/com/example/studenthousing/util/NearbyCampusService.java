package com.example.studenthousing.util;

import com.example.studenthousing.data.local.UniversityData;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.model.University;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class NearbyCampusService {
    public interface NearbyCallback {
        void onResult(List<NearbyCampus> campuses);
    }

    public static List<NearbyCampus> calculateNearby(double lat, double lon) {
        List<NearbyCampus> result = new ArrayList<>();
        if (lat == 0.0 && lon == 0.0) return result;
        List<University> unis = UniversityData.getUniversities();
        for (University u : unis) {
            double km = DistanceCalculator.calculateHaversineKm(lat, lon, u.getLatitude(), u.getLongitude()) * 1.35;
            double rounded = Math.round(km * 10.0) / 10.0;
            result.add(new NearbyCampus(u.getCode(), rounded, true));
        }
        result.sort((a, b) -> Double.compare(a.getKm(), b.getKm()));
        return result;
    }

    /**
     * Calculate nearby universities using road routing (OSRM).
     * Filters results to within 5km.
     * Runs asynchronously and returns results via callback.
     */
    public static void calculateNearbyWithRouting(double lat, double lon, NearbyCallback callback) {
        if (lat == 0.0 && lon == 0.0) {
            callback.onResult(Collections.emptyList());
            return;
        }

        List<University> unis = UniversityData.getUniversities();
        List<NearbyCampus> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(unis.size());

        DistanceCalculator calc = new DistanceCalculator();
        for (University u : unis) {
            double uniLat = u.getLatitude();
            double uniLon = u.getLongitude();
            
            // Quick haversine pre-filter to skip obviously far ones
            double estKm = DistanceCalculator.calculateHaversineKm(lat, lon, uniLat, uniLon) * 1.35;
            if (estKm > 7.0) { // Skip if estimated > 7km (buffer for 5km road distance)
                latch.countDown();
                continue;
            }

            calc.calculateDistance(lat, lon, uniLat, uniLon, new DistanceCalculator.DistanceResultCallback() {
                @Override
                public void onSuccess(double distanceKm, String source, boolean isEstimate) {
                    if (distanceKm <= 5.0) {
                        results.add(new NearbyCampus(u.getCode(), distanceKm, isEstimate));
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    // Fallback to haversine estimate
                    double fallbackKm = Math.round(estKm * 10.0) / 10.0;
                    if (fallbackKm <= 5.0) {
                        results.add(new NearbyCampus(u.getCode(), fallbackKm, true));
                    }
                    latch.countDown();
                }
            });
        }

        new Thread(() -> {
            try {
                latch.await();
                results.sort(Comparator.comparingDouble(NearbyCampus::getKm));
                callback.onResult(results);
            } catch (InterruptedException e) {
                callback.onResult(Collections.emptyList());
            }
        }).start();
    }
}
