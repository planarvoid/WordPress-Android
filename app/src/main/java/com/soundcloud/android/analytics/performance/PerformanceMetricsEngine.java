package com.soundcloud.android.analytics.performance;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>Engine class to send performance events: use it whenever a measurement
 * between two points in time is required.</p>
 *
 * <p>A good example would be application startup time since there is a starting and ending point.</p>
 *
 * See: {@link PerformanceMetricsEngine#startMeasuring(MetricType)}<br>
 * See: {@link PerformanceMetricsEngine#endMeasuring(MetricType)}}<br>
 */
@Singleton
public class PerformanceMetricsEngine {

    private final static String TAG = PerformanceMetricsEngine.class.getSimpleName();

    private final EventBus eventBus;
    private final ConcurrentHashMap<MetricType, List<PerformanceMetric>> metricTypeList = new ConcurrentHashMap<>();

    @Inject
    PerformanceMetricsEngine(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Starts measuring a {@link MetricType} by creating a
     * {@link PerformanceMetric} with the current {@link System#nanoTime()}.
     */
    public void startMeasuring(MetricType type) {
        startMeasuring(PerformanceMetric.create(type));
    }

    /**
     * Starts measuring a {@link PerformanceMetric} that is already timestamped.
     * Any previous existing {@link PerformanceMetric} of the same {@link MetricType}
     * will be removed.
     */
    public void startMeasuring(PerformanceMetric performanceMetric) {
        clearMeasuring(performanceMetric.metricType());
        addPerformanceMetric(performanceMetric);
    }

    /**
     * Ends measuring a {@link MetricType} by creating a
     * {@link PerformanceMetric} with the current {@link System#nanoTime()}.<br>
     *
     * As a result, a new performance event is going to be published.
     */
    public void endMeasuring(MetricType type) {
        endMeasuring(PerformanceMetric.create(type));
    }

    /**
     * Ends measuring a {@link PerformanceMetric} that is already timestamped.<br>
     *
     * As a result, a new performance event is going to be published.
     */
    public void endMeasuring(PerformanceMetric performanceMetric) {
        addPerformanceMetric(performanceMetric);
        publish(performanceMetric.metricType());
    }

    /**
     * Starts and ends measuring from an initial {@link PerformanceMetric} that is
     * already timestamped and a new {@link PerformanceMetric} with the current
     * {@link System#nanoTime()}.<br>
     *
     * <p>As a result, a new performance event is going to be published.</p>
     */
    public void endMeasuringFrom(PerformanceMetric performanceMetric) {
        startMeasuring(performanceMetric);
        endMeasuring(performanceMetric.metricType());
    }

    /**
     * Clear measurings of the given {@link MetricType}
     */
    public void clearMeasuring(MetricType type) {
        metricTypeList.remove(type);
    }

    private void publish(MetricType metricType) {
        if (isValidMetric(metricType)) {
            sendEvent(metricType, calculateDuration(metricType));
        }
        removeMetric(metricType);
    }

    private boolean isValidMetric(MetricType metricType) {
        // metric should have at least 2 metrics of the same type
        // (start and end) in order to compute the duration of the event
        return metricTypeList.get(metricType).size() >= 2;
    }

    private void addPerformanceMetric(PerformanceMetric performanceMetric) {
        final MetricType metricType = performanceMetric.metricType();

        if (!metricTypeList.containsKey(metricType)) {
            metricTypeList.put(metricType, new ArrayList<>());
        }

        metricTypeList.get(metricType).add(performanceMetric);
    }

    private long calculateDuration(MetricType metricType) {
        List<PerformanceMetric> list = metricTypeList.get(metricType);

        if (!list.isEmpty()) {
            long lastTimestamp = list.get(list.size() - 1).timestamp();
            long firstTimestamp = list.get(0).timestamp();
            return TimeUnit.NANOSECONDS.toMillis(lastTimestamp - firstTimestamp);
        }
        return 0;
    }

    private void sendEvent(MetricType metricType, long duration) {
        MetricParams metricParams = buildMetricParams(metricType, duration);
        PerformanceEvent performanceEvent = PerformanceEvent.create(metricType, metricParams);
        eventBus.publish(EventQueue.PERFORMANCE, performanceEvent);
        logPerformanceMetric(metricType, duration);
    }

    private void logPerformanceMetric(MetricType metricType, long duration) {
        Log.d(TAG, metricType.toString() + ": " + duration + "ms");
    }

    private void removeMetric(MetricType metricType) {
        metricTypeList.remove(metricType);
    }

    private MetricParams buildMetricParams(MetricType metricType, long duration) {
        MetricParams metricParams = new MetricParams();

        for (PerformanceMetric performanceMetric : metricTypeList.get(metricType)) {
            metricParams.putAll(performanceMetric.metricParams());
        }

        metricParams.putLong(MetricKey.TIME_MILLIS, duration);

        return metricParams;
    }
}
