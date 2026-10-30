package com.example.studenthousing.util;

import com.example.studenthousing.StudentHousingApp;
import com.example.studenthousing.data.loader.CampusDataLoader;
import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;

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
        List<Campus> campuses = CampusDataLoader.loadCampuses(StudentHousingApp.getInstance());
        for (Campus c : campuses) {
            double km = DistanceCalculator.calculateHaversineKm(lat, lon, c.getLat(), c.getLon()) * 1.35;
            double rounded = Math.round(km * 10.0) / 10.0;
            result.add(new NearbyCampus(c.getCampusCode(), rounded, true));
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

        List<Campus> campuses = CampusDataLoader.loadCampuses(StudentHousingApp.getInstance());
        List<NearbyCampus> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(campuses.size());

        DistanceCalculator calc = new DistanceCalculator();
        for (Campus c : campuses) {
            double uniLat = c.getLat();
            double uniLon = c.getLon();
            
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
                        results.add(new NearbyCampus(c.getCampusCode(), distanceKm, isEstimate));
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    // Fallback to haversine estimate
                    double fallbackKm = Math.round(estKm * 10.0) / 10.0;
                    if (fallbackKm <= 5.0) {
                        results.add(new NearbyCampus(c.getCampusCode(), fallbackKm, true));
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
