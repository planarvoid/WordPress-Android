package com.soundcloud.android.olddiscovery.perf;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;

class DiscoveryAsHomeMeasurements extends DefaultDiscoveryMeasurements {

    private static final String HOME_SCREEN_NAME = Screen.SEARCH_MAIN.get();
    private final PerformanceMetricsEngine performanceMetricsEngine;

    DiscoveryAsHomeMeasurements(PerformanceMetricsEngine performanceMetricsEngine) {
        super(performanceMetricsEngine, HOME_SCREEN_NAME);
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void endLoading() {
        super.endLoading();
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.LOGIN, HOME_SCREEN_NAME));
    }
}
