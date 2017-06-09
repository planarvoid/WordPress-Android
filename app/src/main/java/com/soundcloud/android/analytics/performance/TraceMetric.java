package com.soundcloud.android.analytics.performance;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.soundcloud.java.optional.Optional;

class TraceMetric {
    static final TraceMetric EMPTY = new TraceMetric(Optional.absent());

    private final Optional<Trace> trace;

    private TraceMetric(Optional<Trace> trace) {
        this.trace = trace;
    }

    static TraceMetric create(MetricType metricType) {
        final FirebasePerformance firebase = FirebasePerformance.getInstance();
        if (firebase != null && firebase.isPerformanceCollectionEnabled()) {
            return new TraceMetric(Optional.of(firebase.newTrace(metricType.toString())));
        } else {
            return EMPTY;
        }
    }

    void start() {
        if (trace.isPresent()) {
            trace.get().start();
        }
    }

    void stop() {
        if (trace.isPresent()) {
            trace.get().stop();
        }
    }

    boolean isEmpty() {
        return !trace.isPresent();
    }
}
