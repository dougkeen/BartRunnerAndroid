package com.dougkeen.bart.networktasks;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class NetworkUtils {

    public static OkHttpClient makeHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build();
    }

    static final int CONNECTION_TIMEOUT_MILLIS = 10000;
}
