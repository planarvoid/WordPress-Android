package com.soundcloud.android.analytics.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class PerformanceMetricsEngineTest extends AndroidUnitTest {

    private static final MetricType METRIC_TYPE = MetricType.APP_ON_CREATE;

    private static final PerformanceMetric START_METRIC = PerformanceMetric.builder()
                                                                           .timestamp(TimeUnit.MILLISECONDS.toNanos(1000L))
                                                                           .metricType(METRIC_TYPE)
                                                                           .build();

    private static final PerformanceMetric END_METRIC = PerformanceMetric.builder()
                                                                         .timestamp(TimeUnit.MILLISECONDS.toNanos(2500L))
                                                                         .metricType(METRIC_TYPE)
                                                                         .build();

    private final TestEventBus eventBus = new TestEventBus();

    private PerformanceMetricsEngine performanceMetricsEngine;

    @Before
    public void setUp() throws Exception {
        performanceMetricsEngine = new PerformanceMetricsEngine(eventBus);
    }

    @Test
    public void publishesPerformanceEventWithDuration() {
        performanceMetricsEngine.startMeasuring(START_METRIC);
        performanceMetricsEngine.endMeasuring(END_METRIC);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(durationFromEvent(actualEvent)).isEqualTo(1500L);
    }

    @Test
    public void publishesPerformanceEventFromAPreviousMetric() {
        performanceMetricsEngine.endMeasuringFrom(START_METRIC);

        PerformanceEvent actualEvent = eventBus.lastEventOn(EventQueue.PERFORMANCE);

        assertThat(actualEvent.metricType()).isEqualTo(METRIC_TYPE);
        assertThat(hasDurationKey(actualEvent)).isTrue();
    }

    @Test
    public void doesNotPublishWithOnlyOneMetric() {
        performanceMetricsEngine.endMeasuring(END_METRIC);

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE).isEmpty()).isTrue();
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

        performanceMetricsEngine.clearMeasuring(METRIC_TYPE);
        performanceMetricsEngine.endMeasuring(METRIC_TYPE);

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE).isEmpty()).isTrue();
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
