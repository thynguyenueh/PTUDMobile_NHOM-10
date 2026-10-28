package com.example.studenthousing.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.studenthousing.data.model.province.Province;
import com.example.studenthousing.data.model.province.Ward;
import com.example.studenthousing.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * API Client sử dụng dữ liệu v2 từ provinces.open-api.vn/api/v2/
 * Khóa provinceCode = 79 (TP. Hồ Chí Minh).
 */
public class ProvinceApiClient {

    private static final String BASE_URL_V2 = "https://provinces.open-api.vn/api/v2/";
    private static final String USER_AGENT = "StudentHousingApp/1.0";
    public static final int PROVINCE_CODE_HCM = Constants.TPHCM_CITY_CODE; // 79

    public enum ApiVersion {
        V2
    }

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public ProvinceApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchWards(ApiCallback<List<Ward>> callback) {
        fetchWards(PROVINCE_CODE_HCM, callback);
    }

    public void fetchWards(int provinceCode, ApiCallback<List<Ward>> callback) {
        int targetProvinceCode = Constants.TPHCM_CITY_CODE; // 79
        String url = BASE_URL_V2 + "p/" + targetProvinceCode + "?depth=2";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        postError(callback, new IOException("HTTP Error: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(body.string());
                    JSONArray wardsArray = json.optJSONArray("wards");
                    List<Ward> wards = new ArrayList<>();
                    if (wardsArray != null) {
                        for (int i = 0; i < wardsArray.length(); i++) {
                            JSONObject obj = wardsArray.getJSONObject(i);
                            Ward w = new Ward();
                            w.setCode(obj.optInt("code"));
                            w.setName(obj.optString("name"));
                            w.setDivisionType(obj.optString("division_type"));
                            w.setCodename(obj.optString("codename"));
                            wards.add(w);
                        }
                    }
                    postSuccess(callback, wards);
                } catch (Exception e) {
                    postError(callback, e);
                }
            }
        });
    }

    public void fetchProvinces(ApiCallback<List<Province>> callback) {
        List<Province> provinces = new ArrayList<>();
        provinces.add(new Province(Constants.TPHCM_CITY_CODE, Constants.TPHCM_CITY_NAME));
        postSuccess(callback, provinces);
    }

    public void fetchProvinces(ApiVersion version, ApiCallback<List<Province>> callback) {
        fetchProvinces(callback);
    }

    private <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(ApiCallback<T> callback, Exception e) {
        mainHandler.post(() -> callback.onError(e));
    }
}
