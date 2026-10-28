package com.example.studenthousing.util;

import android.content.Context;

import com.example.studenthousing.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {

    public static String relative(Context context, long ts) {
        if (ts <= 0) return "";
        if (context == null) return relative(ts);
        long diff = System.currentTimeMillis() - ts;
        long min = diff / 60_000;
        if (min < 1) return context.getString(R.string.time_just_now);
        if (min < 60) return context.getString(R.string.time_minutes_ago, (int) min);
        long hr = min / 60;
        if (hr < 24) return context.getString(R.string.time_hours_ago, (int) hr);
        long day = hr / 24;
        if (day == 1) return context.getString(R.string.time_yesterday);
        if (day < 7) return context.getString(R.string.time_days_ago, (int) day);
        if (day < 365)
            return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(ts));
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(ts));
    }

    public static String relative(long ts) {
        if (ts <= 0) return "";
        long diff = System.currentTimeMillis() - ts;
        long min = diff / 60_000;
        if (min < 1) return "Vừa xong";
        if (min < 60) return min + " phút trước";
        long hr = min / 60;
        if (hr < 24) return hr + " giờ trước";
        long day = hr / 24;
        if (day == 1) return "Hôm qua";
        if (day < 7) return day + " ngày trước";
        if (day < 365)
            return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(ts));
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(ts));
    }
}
