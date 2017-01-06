package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.annotations.VisibleForTesting;

@AutoValue
public abstract class PerformanceEvent {

    @VisibleForTesting
    public static final String METRIC_APP_STARTUP_TIME = "app_startup_time";

    public static PerformanceEvent forApplicationStartupTime(boolean isUserLoggedIn,
                                                             long timeMillis,
                                                             String appVersionName,
                                                             String deviceName,
                                                             String androidVersion) {
        return new AutoValue_PerformanceEvent(METRIC_APP_STARTUP_TIME, isUserLoggedIn, timeMillis, appVersionName,
                deviceName, androidVersion);
    }

    public abstract String name();

    public abstract boolean userLoggedIn();

    public abstract long timeMillis();

    public abstract String appVersionName();

    public abstract String deviceName();

    public abstract String androidVersion();
}
