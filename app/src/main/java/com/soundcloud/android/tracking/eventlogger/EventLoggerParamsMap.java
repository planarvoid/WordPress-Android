package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

import java.util.HashMap;

public abstract class EventLoggerParamsMap extends HashMap<String, String> {

    public static interface ExternalKeys {
        String ORIGIN_URL = "context";
        String TRIGGER = "trigger";
        String SOURCE = "source";
        String SOURCE_VERSION = "source_version";
        String EXPLORE_TAG = "exploreTag";
        String SET = "set";
    }

    public EventLoggerParamsMap(int expectedSize){
        super(expectedSize);
    }

    public String toQueryParams() {
        return appendAsQueryParams(new Uri.Builder()).build().getQuery().toString();
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        for (String key : keySet()) {
            builder.appendQueryParameter(key, get(key));
        }
        return builder;
    }

    public String toEventLoggerParams() {
        return appendEventLoggerParams(new Uri.Builder()).build().getQuery().toString();
    }

    protected abstract Uri.Builder appendEventLoggerParams(Uri.Builder builder);
}
