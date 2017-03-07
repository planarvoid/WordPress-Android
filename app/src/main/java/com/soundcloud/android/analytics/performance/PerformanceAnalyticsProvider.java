package com.soundcloud.android.analytics.performance;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.firebase.FirebaseAnalyticsWrapper;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;

import android.os.Bundle;
import android.util.Log;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PerformanceAnalyticsProvider extends DefaultAnalyticsProvider {
    private static final String TAG = PerformanceAnalyticsProvider.class.getSimpleName();
    private static final String DATA_APP_VERSION = "app_version_name";
    private static final String DATA_ANDROID_VERSION = "android_version";

    private final FirebaseAnalyticsWrapper firebaseAnalytics;
    private final FabricReporter fabricReporter;
    private final DeviceHelper deviceHelper;

    @Inject
    PerformanceAnalyticsProvider(FirebaseAnalyticsWrapper firebaseAnalytics,
                                 FabricReporter fabricReporter,
                                 DeviceHelper deviceHelper) {
        this.firebaseAnalytics = firebaseAnalytics;
        this.fabricReporter = fabricReporter;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public void handlePerformanceEvent(PerformanceEvent event) {
        logPerformanceEventInFirebase(event);
        logPerformanceEventInFabric(event);
    }

    private void logPerformanceEventInFirebase(PerformanceEvent event) {
        Log.d(TAG, String.format("Logging performance event in Firebase: %s", event.metricType()));
        firebaseAnalytics.logEvent(event.metricType().toString(), event.metricParams().toBundle());
    }

    private void logPerformanceEventInFabric(PerformanceEvent event) {
        Log.d(TAG, String.format("Logging performance event in Fabric: %s", event.metricType()));
        fabricReporter.post(buildFabricAppStartupMetric(event));
    }

    private Metric buildFabricAppStartupMetric(PerformanceEvent event) {
        Bundle metricParams = event.metricParams().toBundle();
        return Metric.create(event.metricType().toString(), bundleToFabricDataPoints(metricParams));
    }

    private DataPoint[] bundleToFabricDataPoints(Bundle metricParams) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (String key : metricParams.keySet()) {
            Object value = metricParams.get(key);

            if (value instanceof Number) {
                dataPoints.add(DataPoint.numeric(key, (Number) value));
            } else if (value instanceof String) {
                dataPoints.add(DataPoint.string(key, (String) value));
            } else if (value instanceof Boolean) {
                dataPoints.add(DataPoint.string(key, String.valueOf(value)));
            }
        }

        dataPoints.add(DataPoint.string(DATA_APP_VERSION, deviceHelper.getAppVersionName()));
        dataPoints.add(DataPoint.string(DATA_ANDROID_VERSION, deviceHelper.getAndroidReleaseVersion()));

        return dataPoints.toArray(new DataPoint[dataPoints.size()]);
    }
}
