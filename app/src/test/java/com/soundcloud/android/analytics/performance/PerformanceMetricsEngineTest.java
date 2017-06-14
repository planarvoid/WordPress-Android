package com.soundcloud.android.analytics.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class PerformanceMetricsEngineTest extends AndroidUnitTest {

    private static final MetricType METRIC_TYPE = MetricType.DEV_APP_ON_CREATE;

    private final TestEventBus eventBus = new TestEventBus();

    private PerformanceMetricsEngine performanceMetricsEngine;

    private PerformanceMetric earlierStartMetric;
    private PerformanceMetric startMetric;
    private PerformanceMetric endMetric;

    @Mock private TraceMetric traceMetric;

    @Before
    public void setUp() throws Exception {
        performanceMetricsEngine = new PerformanceMetricsEngine(eventBus);
        earlierStartMetric = PerformanceMetric.builder()
                                              .timestamp(TimeUnit.MILLISECONDS.toNanos(500L))
                                              .metricType(METRIC_TYPE)
                                              .traceMetric(traceMetric)
                                              .build();

        startMetric = PerformanceMetric.builder()
                                       .timestamp(TimeUnit.MILLISECONDS.toNanos(1000L))
                                       .metricType(METRIC_TYPE)
                                       .traceMetric(traceMetric)
                                       .build();

        endMetric = PerformanceMetric.builder()
                                     .timestamp(TimeUnit.MILLISECONDS.toNanos(2500L))
                                     .metricType(METRIC_TYPE)
                                     .traceMetric(traceMetric)
                                     .build();
    }

    @Test
    public void publishesPerformanceEventFromAPreviousMetric() {
        performanceMetricsEngine.endMeasuringFrom(startMetric);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(hasDurationKey(actualEvent)).isTrue();
    }

    @Test
    public void publishesPerformanceEventsWithMetricType() {
        performanceMetricsEngine.startMeasuring(METRIC_TYPE);
        performanceMetricsEngine.endMeasuring(METRIC_TYPE);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(hasDurationKey(actualEvent)).isTrue();
    }

    @Test
    public void doesNotPublishClearedMeasurings() {
        performanceMetricsEngine.startMeasuring(METRIC_TYPE);

        performanceMetricsEngine.clearMeasurement(METRIC_TYPE);
        performanceMetricsEngine.endMeasuring(METRIC_TYPE);

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE).isEmpty()).isTrue();
    }

    @Test
    public void publishesPerformanceEventWithDuration() {
        performanceMetricsEngine.startMeasuring(startMetric);
        performanceMetricsEngine.endMeasuring(endMetric);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(durationFromEvent(actualEvent)).isEqualTo(1500L);
    }

    @Test
    public void doesNotPublishWithOnlyOneMetric() {
        performanceMetricsEngine.endMeasuring(endMetric);

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE).isEmpty()).isTrue();
    }

    @Test
    public void shouldClearPreviousPerformanceMetricsOnStart() {
        performanceMetricsEngine.startMeasuring(earlierStartMetric);
        performanceMetricsEngine.startMeasuring(startMetric);
        performanceMetricsEngine.endMeasuring(endMetric);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(durationFromEvent(actualEvent)).isEqualTo(1500L);
    }

    private long durationFromEvent(PerformanceEvent event) {
        return bundleFromEvent(event).getLong(MetricKey.TIME_MILLIS.toString());
    }

    private boolean hasDurationKey(PerformanceEvent event) {
        return bundleFromEvent(event).containsKey(MetricKey.TIME_MILLIS.toString());
    }

    private Bundle bundleFromEvent(PerformanceEvent event) {
        return event.metricParams().toBundle();
    }

}
