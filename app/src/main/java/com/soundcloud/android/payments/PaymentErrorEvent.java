package com.soundcloud.android.payments;

import com.soundcloud.android.events.MetricEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

public class PaymentErrorEvent extends TrackingEvent implements MetricEvent {

    public static final String KIND = "PaymentError";
    private final String errorType;

    public PaymentErrorEvent(String errorType) {
        super(KIND, System.currentTimeMillis());
        this.errorType = errorType;
    }

    @Override
    public Metric toMetric() {
        return Metric.create(KIND, DataPoint.string("Error Type", errorType));
    }
}
