package com.example.studenthousing.data.repository;

import com.example.studenthousing.data.model.province.Province;
import com.example.studenthousing.data.model.province.Ward;
import com.example.studenthousing.data.remote.ProvinceApiClient;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.LocationData;

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
        if (cachedWards != null && !cachedWards.isEmpty()) {
            callback.onSuccess(cachedWards);
            return;
        }

        apiClient.fetchWards(new ProvinceApiClient.ApiCallback<List<Ward>>() {
            @Override
            public void onSuccess(List<Ward> result) {
                if (result != null && !result.isEmpty()) {
                    cachedWards = result;
                    callback.onSuccess(result);
                } else {
                    useFallbackWards(callback);
                }
            }

            @Override
            public void onError(Exception e) {
                useFallbackWards(callback);
            }
        });
    }

    private void useFallbackProvinces(Callback<List<Province>> callback) {
        List<Province> list = new ArrayList<>();
        list.add(new Province(Constants.TPHCM_CITY_CODE, Constants.TPHCM_CITY_NAME));
        cachedProvinces = list;
        callback.onSuccess(list);
    }

    private void useFallbackWards(Callback<List<Ward>> callback) {
        List<Ward> list = new ArrayList<>();
        int code = 27256;
        for (String wName : LocationData.WARDS) {
            list.add(new Ward(code++, wName, "phuong", null));
        }
        cachedWards = list;
        callback.onSuccess(list);
    }
}
