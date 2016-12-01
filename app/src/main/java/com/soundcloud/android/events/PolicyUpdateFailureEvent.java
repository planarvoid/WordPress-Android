package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;
import org.jetbrains.annotations.NotNull;

public class PolicyUpdateFailureEvent extends LegacyTrackingEvent implements MetricEvent {

    public static final String KIND_POLICY_FETCH_FAILED = "PolicyFetch";
    public static final String KIND_POLICY_WRITE_FAILED = "PolicyWrite";
    public static final String CONTEXT_BACKGROUND = "Background";
    public static final String CONTEXT_UPSELL = "Upsell";
    private final String context;

    public static PolicyUpdateFailureEvent fetchFailed(boolean inBackground) {
        return new PolicyUpdateFailureEvent(KIND_POLICY_FETCH_FAILED,
                                            inBackground ? CONTEXT_BACKGROUND : CONTEXT_UPSELL);
    }

    public static PolicyUpdateFailureEvent insertFailed(boolean inBackground) {
        return new PolicyUpdateFailureEvent(KIND_POLICY_WRITE_FAILED,
                                            inBackground ? CONTEXT_BACKGROUND : CONTEXT_UPSELL);
    }

    protected PolicyUpdateFailureEvent(@NotNull String reason, String context) {
        super(reason);
        this.context = context;
    }

    @Override
    public Metric toMetric() {
        return Metric.create("PolicyUpdateFailure",
                             DataPoint.string("Reason", kind),
                             DataPoint.string("Context", context));
    }
}
