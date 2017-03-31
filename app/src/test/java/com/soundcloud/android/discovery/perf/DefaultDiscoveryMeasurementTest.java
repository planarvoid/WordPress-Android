package com.soundcloud.android.discovery.perf;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class DefaultDiscoveryMeasurementTest extends AndroidUnitTest {

    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private DefaultDiscoveryMeasurement defaultStreamMeasurement;

    @Before
    public void setUp() throws Exception {
        defaultStreamMeasurement = new DefaultDiscoveryMeasurement(performanceMetricsEngine);
    }

    @Test
    public void shouldStartLoadHomeMeasurementOnStartRefreshing() {

        defaultStreamMeasurement.startRefreshing();

        verify(performanceMetricsEngine).startMeasuring(MetricType.REFRESH_HOME);
    }

    @Test
    public void shouldEndLoadHomeMeasurementOnEndRefreshing() {

        defaultStreamMeasurement.endRefreshing();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        Assertions.assertThat(performanceMetricArgumentCaptor.getValue())
                  .hasMetricType(MetricType.REFRESH_HOME)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.SEARCH_MAIN.get());
    }

    @Test
    public void shouldEndLoginMeasurementOnEndLoading() {

        defaultStreamMeasurement.endLoading();

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        Assertions.assertThat(performanceMetricArgumentCaptor.getValue())
                  .hasMetricType(MetricType.LOGIN)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.SEARCH_MAIN.get());
    }

}
