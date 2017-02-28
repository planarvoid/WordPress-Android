package com.soundcloud.android.analytics.performance;


import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.firebase.FirebaseAnalyticsWrapper;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;

import android.os.Bundle;
import android.util.Log;

import javax.inject.Inject;

public class PerformanceAnalyticsProvider extends DefaultAnalyticsProvider {
    private static final String TAG = PerformanceAnalyticsProvider.class.getSimpleName();

    @VisibleForTesting
    static final String DATA_IS_USER_LOGGED_IN = "logged_in_user";
    @VisibleForTesting
    static final String DATA_TIME_MILLIS = "time_in_millis";
    @VisibleForTesting
    static final String DATA_APP_VERSION = "app_version_name";
    @VisibleForTesting
    static final String DATA_DEVICE_NAME = "device_type_name";
    @VisibleForTesting
    static final String DATA_ANDROID_VERSION = "android_version";

    private final FirebaseAnalyticsWrapper firebaseAnalytics;
    private final FabricReporter fabricReporter;

    @Inject
    PerformanceAnalyticsProvider(FirebaseAnalyticsWrapper firebaseAnalytics, FabricReporter fabricReporter) {
        this.firebaseAnalytics = firebaseAnalytics;
        this.fabricReporter = fabricReporter;
    }

    @Override
    public void handlePerformanceEvent(PerformanceEvent event) {
        logPerformanceEventInFirebase(event);
        logPerformanceEventInFabric(event);
    }

    private void logPerformanceEventInFirebase(PerformanceEvent event) {
        Log.d(TAG, String.format("Logging performance event in Firebase: %s", event.name()));
        firebaseAnalytics.logEvent(event.name(), buildFirebaseEventData(event));
    }

    private void logPerformanceEventInFabric(PerformanceEvent event) {
        Log.d(TAG, String.format("Logging performance event in Fabric: %s", event.name()));
        fabricReporter.post(buildFabricAppStartupMetric(event));
    }

    @VisibleForTesting
    Bundle buildFirebaseEventData(PerformanceEvent event) {
        final Bundle data = new Bundle();
        data.putBoolean(DATA_IS_USER_LOGGED_IN, event.userLoggedIn());
        data.putLong(DATA_TIME_MILLIS, event.timeMillis());
        data.putString(DATA_APP_VERSION, event.appVersionName());
        data.putString(DATA_DEVICE_NAME, event.deviceName());
        data.putString(DATA_ANDROID_VERSION, event.androidVersion());
        return data;
    }

    @VisibleForTesting
    Metric buildFabricAppStartupMetric(PerformanceEvent event) {
        final DataPoint[] dataPoints = new DataPoint[] {
                DataPoint.numeric(DATA_TIME_MILLIS, event.timeMillis()),
                DataPoint.string(DATA_IS_USER_LOGGED_IN, String.valueOf(event.userLoggedIn())),
                DataPoint.string(DATA_APP_VERSION, event.appVersionName()),
                DataPoint.string(DATA_ANDROID_VERSION, event.androidVersion())
        };
        return Metric.create("AppStartupTime", dataPoints);
    }
}
