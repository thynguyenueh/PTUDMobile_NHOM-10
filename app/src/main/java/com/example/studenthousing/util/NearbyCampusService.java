package com.example.studenthousing.util;

import android.content.Context;

import com.example.studenthousing.data.model.Campus;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.data.repository.CampusRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class NearbyCampusService {
    public interface NearbyCallback {
        void onResult(List<NearbyCampus> campuses);
    }

    public static List<NearbyCampus> calculateNearby(Context context, double lat, double lon) {
        List<NearbyCampus> result = new ArrayList<>();
        if ((lat == 0.0 && lon == 0.0) || context == null) return result;
        List<Campus> campuses = CampusRepository.getInstance(context).getAllCampuses();
        for (Campus c : campuses) {
            if (c == null || (c.getLat() == 0.0 && c.getLon() == 0.0)) continue;
            double km = DistanceCalculator.calculateHaversineKm(lat, lon, c.getLat(), c.getLon()) * 1.35;
            double rounded = Math.round(km * 10.0) / 10.0;
            result.add(new NearbyCampus(c.getCode(), rounded, true));
        }
        result.sort((a, b) -> Double.compare(a.getKm(), b.getKm()));
        return result;
    }

    public static void calculateNearbyWithRouting(Context context, double lat, double lon, NearbyCallback callback) {
        if ((lat == 0.0 && lon == 0.0) || context == null) {
            callback.onResult(Collections.emptyList());
            return;
        }

        List<Campus> campuses = CampusRepository.getInstance(context).getAllCampuses();
        List<NearbyCampus> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(campuses.size());

        DistanceCalculator calc = new DistanceCalculator();
        for (Campus c : campuses) {
            if (c == null || (c.getLat() == 0.0 && c.getLon() == 0.0)) {
                latch.countDown();
                continue;
            }
            double cLat = c.getLat();
            double cLon = c.getLon();

            double estKm = DistanceCalculator.calculateHaversineKm(lat, lon, cLat, cLon) * 1.35;
            if (estKm > 7.0) {
                latch.countDown();
                continue;
            }

            calc.calculateDistance(lat, lon, cLat, cLon, new DistanceCalculator.DistanceResultCallback() {
                @Override
                public void onSuccess(double distanceKm, String source, boolean isEstimate) {
                    if (distanceKm <= 5.0) {
                        results.add(new NearbyCampus(c.getCode(), distanceKm, isEstimate));
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    double fallbackKm = Math.round(estKm * 10.0) / 10.0;
                    if (fallbackKm <= 5.0) {
                        results.add(new NearbyCampus(c.getCode(), fallbackKm, true));
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
