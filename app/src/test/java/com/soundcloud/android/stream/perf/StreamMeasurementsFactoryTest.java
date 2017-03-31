package com.soundcloud.android.stream.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamMeasurementsFactoryTest {

    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    private StreamMeasurementsFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new StreamMeasurementsFactory(defaultHomeScreenConfiguration, performanceMetricsEngine);
    }

    @Test
    public void shouldReturnNoOpStreamMeasurementsWhenStreamIsNotHome() {
        when(defaultHomeScreenConfiguration.isStreamHome()).thenReturn(false);

        StreamMeasurements discoveryMeasurements = factory.create();

        assertThat(discoveryMeasurements.getClass()).isAssignableFrom(NoOpStreamMeasurements.class);
    }

    @Test
    public void shouldReturnDefaultStreamMeasurementsWhenStreamIsHome() {
        when(defaultHomeScreenConfiguration.isStreamHome()).thenReturn(true);

        StreamMeasurements discoveryMeasurements = factory.create();

        assertThat(discoveryMeasurements.getClass()).isAssignableFrom(DefaultStreamMeasurement.class);
    }
}
