package com.dougkeen.bart.networktasks;

import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    private static class RetryInterceptor implements Interceptor {
        private static final String TAG = "RetryInterceptor";

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            // try the request
            Response response;
            int attempt = 0;
            do {
                attempt++;
                try {
                    response = chain.proceed(request);
                } catch (IOException e) {
                    Log.w(TAG, "Request failed: ", e);
                    response = null;
                }
            } while ((response == null || !response.isSuccessful()) && attempt < 2);

            return response;
        }
    }

    public static OkHttpClient makeHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.interceptors().add(new RetryInterceptor());
        return client;
    }

    static final int CONNECTION_TIMEOUT_MILLIS = 10000;
}
