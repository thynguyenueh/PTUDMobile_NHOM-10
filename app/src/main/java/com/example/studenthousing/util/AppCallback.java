package com.example.studenthousing.util;

public interface AppCallback<T> {
    void onSuccess(T data);
    void onError(Exception e);
}
