package com.soundcloud.android.payments;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.MetricEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
abstract class PaymentErrorEvent extends TrackingEvent implements MetricEvent {

    public static final String KIND = "PaymentError";
    abstract String errorType();

    public static PaymentErrorEvent create(String errorType) {
        return new AutoValue_PaymentErrorEvent(defaultId(), defaultTimestamp(), Optional.absent(), errorType);
    }

    @Override
    public PaymentErrorEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PaymentErrorEvent(id(), timestamp(), Optional.of(referringEvent), errorType());
    }

    @Override
    public Metric toMetric() {
        return Metric.create(KIND, DataPoint.string("Error Type", errorType()));
    }
}
