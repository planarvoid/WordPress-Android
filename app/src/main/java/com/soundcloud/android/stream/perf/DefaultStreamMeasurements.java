package com.soundcloud.android.stream.perf;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;

import android.support.annotation.CallSuper;

class DefaultStreamMeasurements implements StreamMeasurements {

    private PerformanceMetricsEngine performanceMetricsEngine;
    private String homeScreenName;

    DefaultStreamMeasurements(PerformanceMetricsEngine performanceMetricsEngine, String homeScreenName) {
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.homeScreenName = homeScreenName;
    }

    @Override
    @CallSuper
    public void startRefreshing() {
        performanceMetricsEngine.startMeasuring(MetricType.STREAM_REFRESH);
    }

    @Override
    @CallSuper
    public void endRefreshing() {
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.STREAM_REFRESH, homeScreenName));
    }

    @Override
    @CallSuper
    public void startLoading() {
        performanceMetricsEngine.startMeasuring(MetricType.STREAM_LOAD);
    }

    @Override
    @CallSuper
    public void endLoading() {
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.STREAM_LOAD, homeScreenName));
    }

    protected static PerformanceMetric createPerformanceMetric(MetricType metricType, String homeScreenName) {
        MetricParams params = MetricParams.of(MetricKey.HOME_SCREEN, homeScreenName);
        return PerformanceMetric.builder()
                                .metricType(metricType)
                                .metricParams(params)
                                .build();
    }

}
