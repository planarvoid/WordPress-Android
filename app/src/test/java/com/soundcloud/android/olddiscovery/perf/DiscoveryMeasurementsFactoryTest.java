package com.soundcloud.android.olddiscovery.perf;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryMeasurementsFactoryTest {

    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    private DiscoveryMeasurementsFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new DiscoveryMeasurementsFactory(defaultHomeScreenConfiguration, performanceMetricsEngine);
    }

    @Test
    public void shouldReturnNoOpDiscoveryMeasurementsWhenDiscoveryIsNotHome() {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(false);

        DiscoveryMeasurements discoveryMeasurements = factory.create();

        assertThat(discoveryMeasurements.getClass()).isAssignableFrom(DefaultDiscoveryMeasurements.class);
    }

    @Test
    public void shouldReturnDefaultDiscoveryMeasurementsWhenDiscoveryIsHome() {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(true);

        DiscoveryMeasurements discoveryMeasurements = factory.create();

        assertThat(discoveryMeasurements.getClass()).isAssignableFrom(DiscoveryAsHomeMeasurements.class);
    }
}
