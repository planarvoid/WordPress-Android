package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class PaymentFailureEvent extends TrackingEvent implements MetricEvent {

    private static final String PAYMENT_FAIL_METRIC = "Payment failure";
    public abstract String reason();

    public static PaymentFailureEvent create(String reason) {
        return new AutoValue_PaymentFailureEvent(defaultId(), defaultTimestamp(), Optional.absent(), reason);
    }

    @Override
    public String toString() {
        return "Payment failure: " + reason();
    }

    @Override
    public PaymentFailureEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PaymentFailureEvent(id(), timestamp(), Optional.of(referringEvent), reason());
    }

    @Override
    public Metric toMetric() {
        return Metric.create(PAYMENT_FAIL_METRIC, DataPoint.string("Reason", reason()));
    }
}
