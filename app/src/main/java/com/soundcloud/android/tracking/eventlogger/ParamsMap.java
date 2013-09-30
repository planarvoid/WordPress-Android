package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

import java.util.HashMap;

public class ParamsMap extends HashMap<String, String> {

    public String toQueryParams() {
        return appendAsQueryParams(new Uri.Builder()).build().getQuery().toString();
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        for (String key : keySet()) {
            builder.appendQueryParameter(key, get(key));
        }
        return builder;
    }
}
