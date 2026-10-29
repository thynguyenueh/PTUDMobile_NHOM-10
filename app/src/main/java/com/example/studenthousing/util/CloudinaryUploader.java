package com.example.studenthousing.util;

import java.io.File;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class CloudinaryUploader {
    private static final String CLOUD_NAME = "gomspy8v";
    private static final String UPLOAD_PRESET = "studenthousing_unsigned";

    public static void uploadImage(File imageFile, AppCallback<String> callback) {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/*")))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);
                        String imageUrl = obj.getString("secure_url");
                        callback.onSuccess(imageUrl);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                } else {
                    callback.onError(new Exception("Upload failed: " + response.code()));
                }
            }
        });
    }
}
