package com.soundcloud.android.stream.perf;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;

import javax.inject.Inject;

public class StreamMeasurementsFactory {

    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public StreamMeasurementsFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration,
                                     PerformanceMetricsEngine performanceMetricsEngine) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public StreamMeasurements create() {
        if (defaultHomeScreenConfiguration.isStreamHome()) {
            return new DefaultStreamMeasurement(performanceMetricsEngine);
        } else {
            return new NoOpStreamMeasurements();
        }
    }
}
