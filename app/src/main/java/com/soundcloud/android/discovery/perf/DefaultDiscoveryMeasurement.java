package com.soundcloud.android.discovery.perf;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;

class DefaultDiscoveryMeasurement implements DiscoveryMeasurements {
    private PerformanceMetricsEngine performanceMetricsEngine;

    DefaultDiscoveryMeasurement(PerformanceMetricsEngine performanceMetricsEngine) {
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void startRefreshing() {
        performanceMetricsEngine.startMeasuring(MetricType.REFRESH_HOME);
    }

    @Override
    public void endRefreshing() {
        MetricParams params = MetricParams.of(MetricKey.HOME_SCREEN, Screen.SEARCH_MAIN.get());
        performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                               .metricType(MetricType.REFRESH_HOME)
                                                               .metricParams(params)
                                                               .build());
    }

    @Override
    public void endLoading() {
        MetricParams params = MetricParams.of(MetricKey.HOME_SCREEN, Screen.SEARCH_MAIN.get());
        performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                               .metricType(MetricType.LOGIN)
                                                               .metricParams(params)
                                                               .build());
    }

}
