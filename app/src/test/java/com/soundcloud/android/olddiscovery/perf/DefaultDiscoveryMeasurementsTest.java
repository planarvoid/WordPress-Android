package com.soundcloud.android.olddiscovery.perf;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class DefaultDiscoveryMeasurementsTest extends AndroidUnitTest {

    private static final String SCREEN_NAME = "foo";

    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private DefaultDiscoveryMeasurements defaultDiscoveryMeasurements;

    @Before
    public void setUp() throws Exception {
        defaultDiscoveryMeasurements = new DefaultDiscoveryMeasurements(performanceMetricsEngine, SCREEN_NAME);
    }

    @Test
    public void shouldStartMeasuringDiscoveryRefreshOnStartRefreshing() {
        defaultDiscoveryMeasurements.startRefreshing();

        verify(performanceMetricsEngine).startMeasuring(MetricType.DISCOVERY_REFRESH);
    }

    @Test
    public void shouldEndMeasuringDiscoveryRefreshOnEndRefreshing() {
        defaultDiscoveryMeasurements.endRefreshing();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        PerformanceMetric value = performanceMetricArgumentCaptor.getValue();
        Assertions.assertThat(value)
                  .hasMetricType(MetricType.DISCOVERY_REFRESH)
                  .containsMetricParam(MetricKey.HOME_SCREEN, SCREEN_NAME);
    }

    @Test
    public void shouldStartMeasuringDiscoveryLoadOnStartLoading() {
        defaultDiscoveryMeasurements.startLoading();

        verify(performanceMetricsEngine).startMeasuring(MetricType.DISCOVERY_LOAD);
    }

    @Test
    public void shouldEndMeasuringDiscoveryLoadOnEndLoading() {
        defaultDiscoveryMeasurements.endLoading();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        PerformanceMetric value = performanceMetricArgumentCaptor.getValue();
        Assertions.assertThat(value)
                  .hasMetricType(MetricType.DISCOVERY_LOAD)
                  .containsMetricParam(MetricKey.HOME_SCREEN, SCREEN_NAME);
    }
}
