package com.example.studenthousing.data.repository;

import com.example.studenthousing.StudentHousingApp;
import com.example.studenthousing.data.loader.WardsDataLoader;
import com.example.studenthousing.data.model.Province;
import com.example.studenthousing.data.model.Ward;
import com.example.studenthousing.data.remote.ProvinceApiClient;
import com.example.studenthousing.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý danh mục hành chính (khóa TP.HCM - provinceCode = 79, v2).
 */
public class ProvinceRepository {

    private static ProvinceRepository instance;
    private final ProvinceApiClient apiClient;

    private List<Province> cachedProvinces = null;
    private List<Ward> cachedWards = null;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    private ProvinceRepository() {
        this.apiClient = new ProvinceApiClient();
    }

    public static synchronized ProvinceRepository getInstance() {
        if (instance == null) {
            instance = new ProvinceRepository();
        }
        return instance;
    }

    public void getProvinces(Callback<List<Province>> callback) {
        if (cachedProvinces != null && !cachedProvinces.isEmpty()) {
            callback.onSuccess(cachedProvinces);
            return;
        }

        apiClient.fetchProvinces(new ProvinceApiClient.ApiCallback<List<Province>>() {
            @Override
            public void onSuccess(List<Province> result) {
                if (result != null && !result.isEmpty()) {
                    cachedProvinces = result;
                    callback.onSuccess(result);
                } else {
                    useFallbackProvinces(callback);
                }
            }

            @Override
            public void onError(Exception e) {
                useFallbackProvinces(callback);
            }
        });
    }

    public void getProvinces(ProvinceApiClient.ApiVersion version, Callback<List<Province>> callback) {
        getProvinces(callback);
    }

    public void getWards(Callback<List<Ward>> callback) {
        List<Ward> list = WardsDataLoader.loadWards(StudentHousingApp.getInstance());
        cachedWards = list != null ? list : new ArrayList<>();
        callback.onSuccess(cachedWards);
    }

    private void useFallbackProvinces(Callback<List<Province>> callback) {
        List<Province> list = new ArrayList<>();
        list.add(new Province(Constants.TPHCM_CITY_CODE, Constants.TPHCM_CITY_NAME));
        cachedProvinces = list;
        callback.onSuccess(list);
    }
}
