package com.soundcloud.android.stream.perf;

import static org.mockito.Mockito.times;
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

import java.util.List;

public class StreamAsHomeMeasurementsTest extends AndroidUnitTest {

    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private StreamAsHomeMeasurements streamAsHomeMeasurements;

    @Before
    public void setUp() throws Exception {
        streamAsHomeMeasurements = new StreamAsHomeMeasurements(performanceMetricsEngine);
    }

    @Test
    public void shouldEndMeasuringLoadingAndLoginTimeOnEndLoading() {
        streamAsHomeMeasurements.endLoading();

        verify(performanceMetricsEngine, times(2)).endMeasuring(performanceMetricArgumentCaptor.capture());

        List<PerformanceMetric> values = performanceMetricArgumentCaptor.getAllValues();

        Assertions.assertThat(values.get(0))
                  .hasMetricType(MetricType.STREAM_LOAD)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.STREAM.get());

        Assertions.assertThat(values.get(1))
                  .hasMetricType(MetricType.LOGIN)
                  .containsMetricParam(MetricKey.HOME_SCREEN, Screen.STREAM.get());
    }


}
