package com.soundcloud.android.configuration;

import com.soundcloud.android.events.MetricEvent;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

import java.util.Locale;

public final class ForceUpdateEvent implements MetricEvent {

    private final String androidVersion;
    private final String appVersionName;
    private final int appVersionCode;

    public ForceUpdateEvent(String androidVersion, String appVersionName, int appVersionCode) {
        this.androidVersion = androidVersion;
        this.appVersionName = appVersionName;
        this.appVersionCode = appVersionCode;
    }

    @Override
    public Metric toMetric() {
        return Metric.create("ForceUpdate",
                DataPoint.string("Platform version", androidVersion),
                DataPoint.string("App version", formattedVersion(appVersionName, appVersionCode)));
    }

    private static String formattedVersion(String appVersionName, int appVersionCode) {
        return String.format(Locale.US, "%s (%d)", appVersionName, appVersionCode);
    }
}
