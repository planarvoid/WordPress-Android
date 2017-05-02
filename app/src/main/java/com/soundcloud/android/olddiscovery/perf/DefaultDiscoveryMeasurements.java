package com.soundcloud.android.olddiscovery.perf;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;

import android.support.annotation.CallSuper;

class DefaultDiscoveryMeasurements implements DiscoveryMeasurements {

    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final String homeScreenName;

    DefaultDiscoveryMeasurements(PerformanceMetricsEngine performanceMetricsEngine, String homeScreenName) {
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.homeScreenName = homeScreenName;
    }

    @Override
    @CallSuper
    public void startRefreshing() {
        performanceMetricsEngine.startMeasuring(MetricType.DISCOVERY_REFRESH);
    }

    @Override
    @CallSuper
    public void endRefreshing() {
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.DISCOVERY_REFRESH, homeScreenName));
    }

    @Override
    @CallSuper
    public void startLoading() {
        performanceMetricsEngine.startMeasuring(MetricType.DISCOVERY_LOAD);
    }

    @Override
    @CallSuper
    public void endLoading() {
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.DISCOVERY_LOAD, homeScreenName));
    }

    protected static PerformanceMetric createPerformanceMetric(MetricType metricType, String homeScreenName) {
        MetricParams params = MetricParams.of(MetricKey.HOME_SCREEN, homeScreenName);
        return PerformanceMetric.builder()
                                .metricType(metricType)
                                .metricParams(params)
                                .build();
    }
}
