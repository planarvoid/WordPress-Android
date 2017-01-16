package com.soundcloud.android.events;


import static com.soundcloud.android.events.PolicyUpdateFailureEvent.Context.CONTEXT_BACKGROUND;
import static com.soundcloud.android.events.PolicyUpdateFailureEvent.Context.CONTEXT_UPSELL;
import static com.soundcloud.android.events.PolicyUpdateFailureEvent.Reason.KIND_POLICY_FETCH_FAILED;
import static com.soundcloud.android.events.PolicyUpdateFailureEvent.Reason.KIND_POLICY_WRITE_FAILED;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class PolicyUpdateFailureEvent extends NewTrackingEvent implements MetricEvent {
    public enum Reason {

        KIND_POLICY_FETCH_FAILED("PolicyFetch"),
        KIND_POLICY_WRITE_FAILED("PolicyWrite");
        private final String key;

        Reason(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }
    enum Context {

        CONTEXT_BACKGROUND("Background"),
        CONTEXT_UPSELL("Upsell");
        private final String key;

        Context(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }
    public abstract Reason reason();
    abstract Context context();

    public static PolicyUpdateFailureEvent fetchFailed(boolean inBackground) {
        return PolicyUpdateFailureEvent.create(KIND_POLICY_FETCH_FAILED, inBackground ? CONTEXT_BACKGROUND : CONTEXT_UPSELL);
    }

    public static PolicyUpdateFailureEvent insertFailed(boolean inBackground) {
        return PolicyUpdateFailureEvent.create(KIND_POLICY_WRITE_FAILED, inBackground ? CONTEXT_BACKGROUND : CONTEXT_UPSELL);
    }

    private static PolicyUpdateFailureEvent create(Reason reason, Context context) {
        return new AutoValue_PolicyUpdateFailureEvent(defaultId(), defaultTimestamp(), Optional.absent(), reason, context);
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PolicyUpdateFailureEvent(id(), timestamp(), Optional.of(referringEvent), reason(), context());
    }

    @Override
    public Metric toMetric() {
        return Metric.create("PolicyUpdateFailure", DataPoint.string("Reason", reason().toString()), DataPoint.string("Context", context().toString()));
    }
}
