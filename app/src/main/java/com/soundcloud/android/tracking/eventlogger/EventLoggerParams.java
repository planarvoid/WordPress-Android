package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

public interface EventLoggerParams {

    public static interface ExternalKeys {
        String ORIGIN_URL = "context";
        String TRIGGER = "trigger";
        String SOURCE = "source";
        String SOURCE_VERSION = "source_version";
        String EXPLORE_TAG = "exploreTag";
        String SET = "set";
    }

    public Uri.Builder appendEventLoggerParams(Uri.Builder builder);
}
