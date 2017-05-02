package com.soundcloud.android.stream.perf;

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

public class DefaultStreamMeasurementsTest extends AndroidUnitTest {

    private static final String SCREEN_NAME = "foo";
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private DefaultStreamMeasurements defaultStreamMeasurements;

    @Before
    public void setUp() throws Exception {
        defaultStreamMeasurements = new DefaultStreamMeasurements(performanceMetricsEngine, SCREEN_NAME);
    }

    @Test
    public void shouldStartMeasuringDiscoveryRefreshOnStartRefreshing() {
        defaultStreamMeasurements.startRefreshing();

        verify(performanceMetricsEngine).startMeasuring(MetricType.STREAM_REFRESH);
    }

    @Test
    public void shouldEndMeasuringDiscoveryRefreshOnEndRefreshing() {
        defaultStreamMeasurements.endRefreshing();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        PerformanceMetric value = performanceMetricArgumentCaptor.getValue();
        Assertions.assertThat(value)
                  .hasMetricType(MetricType.STREAM_REFRESH)
                  .containsMetricParam(MetricKey.HOME_SCREEN, SCREEN_NAME);
    }

    @Test
    public void shouldStartMeasuringDiscoveryLoadOnStartLoading() {
        defaultStreamMeasurements.startLoading();

        verify(performanceMetricsEngine).startMeasuring(MetricType.STREAM_LOAD);
    }

    @Test
    public void shouldEndMeasuringDiscoveryLoadOnEndLoading() {
        defaultStreamMeasurements.endLoading();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());

        PerformanceMetric value = performanceMetricArgumentCaptor.getValue();
        Assertions.assertThat(value)
                  .hasMetricType(MetricType.STREAM_LOAD)
                  .containsMetricParam(MetricKey.HOME_SCREEN, SCREEN_NAME);
    }

}
