package com.example.studenthousing.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {
    public static File getFileFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
            File existing = new File(uri.getPath());
            if (existing.exists()) return existing;
        }
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = File.createTempFile("upload_", ".jpg", context.getFilesDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveUriToLocalFile(Context context, Uri uri) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.toString();
        }
        File file = getFileFromUri(context, uri);
        return file != null ? Uri.fromFile(file).toString() : uri.toString();
    }

    public static void deleteLocalFile(Context context, String uriOrPath) {
        if (context == null || uriOrPath == null) return;
        try {
            Uri uri = Uri.parse(uriOrPath);
            if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
                File file = new File(uri.getPath());
                if (file.exists() && file.getAbsolutePath().startsWith(context.getFilesDir().getAbsolutePath())) {
                    file.delete();
                }
            }
        } catch (Exception ignored) {}
    }
}
