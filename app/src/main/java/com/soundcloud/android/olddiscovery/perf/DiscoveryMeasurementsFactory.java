package com.soundcloud.android.olddiscovery.perf;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;

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
            return new DiscoveryAsHomeMeasurements(performanceMetricsEngine);
        } else {
            return new DefaultDiscoveryMeasurements(performanceMetricsEngine, defaultHomeScreenConfiguration.screenName());
        }
    }
}
