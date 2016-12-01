package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

public class PaymentFailureEvent extends LegacyTrackingEvent implements MetricEvent {

    private static final String PAYMENT_FAIL_METRIC = "Payment failure";
    private static final String KEY_REASON = "reason";

    public static PaymentFailureEvent create(String reason) {
        return new PaymentFailureEvent(reason);
    }

    private PaymentFailureEvent(String reason) {
        super(LegacyTrackingEvent.KIND_DEFAULT);
        put(KEY_REASON, reason);
    }

    public String getReason() {
        return get(KEY_REASON);
    }

    @Override
    public String toString() {
        return "Payment failure: " + getReason();
    }

    @Override
    public Metric toMetric() {
        return Metric.create(PAYMENT_FAIL_METRIC, DataPoint.string("Reason", getReason()));
    }
}
