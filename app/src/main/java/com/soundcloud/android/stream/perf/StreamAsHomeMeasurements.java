package com.soundcloud.android.stream.perf;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;

class StreamAsHomeMeasurements extends DefaultStreamMeasurements {

    private static final String SCREEN_NAME = Screen.STREAM.get();
    private final PerformanceMetricsEngine performanceMetricsEngine;

    StreamAsHomeMeasurements(PerformanceMetricsEngine performanceMetricsEngine) {
        super(performanceMetricsEngine, SCREEN_NAME);
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void endLoading() {
        super.endLoading();
        performanceMetricsEngine.endMeasuring(createPerformanceMetric(MetricType.LOGIN, SCREEN_NAME));
    }
}
