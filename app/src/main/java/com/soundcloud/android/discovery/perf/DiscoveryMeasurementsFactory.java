package com.soundcloud.android.discovery.perf;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;

import javax.inject.Inject;

public class DiscoveryMeasurementsFactory {

    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public DiscoveryMeasurementsFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration,
                                        PerformanceMetricsEngine performanceMetricsEngine) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public DiscoveryMeasurements create() {
        if (defaultHomeScreenConfiguration.isDiscoveryHome()) {
            return new DefaultDiscoveryMeasurement(performanceMetricsEngine);
        } else {
            return new NoOpDiscoveryMeasurements();
        }
    }
}
