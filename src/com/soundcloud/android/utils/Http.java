package com.soundcloud.android.utils;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.http.AndroidHttpClient;
import android.os.Build;

public class Http {

    public static HttpClient createHttpClient(String userAgent) {
        if (Build.VERSION.SDK_INT >= 8) {
            return AndroidHttpClient.newInstance(userAgent);
        } else {
            return new DefaultHttpClient();
        }
    }


    public static void close(HttpClient client) {
        if (client instanceof AndroidHttpClient) {
            // avoid leak error logging
            ((AndroidHttpClient) client).close();
        } else if (client != null) {
            client.getConnectionManager().shutdown();
        }
    }
}
