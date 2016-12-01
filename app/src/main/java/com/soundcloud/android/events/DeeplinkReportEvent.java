package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;
import org.jetbrains.annotations.NotNull;

public class DeeplinkReportEvent extends LegacyTrackingEvent implements MetricEvent {

    private static final String KIND_SUCCESS = "Success";
    private static final String KIND_FAILURE = "Failed";
    private final String referrer;

    public static DeeplinkReportEvent forResolvedDeeplink(String referrer) {
        return new DeeplinkReportEvent(KIND_SUCCESS, referrer);
    }

    public static DeeplinkReportEvent forResolutionFailure(String referrer) {
        return new DeeplinkReportEvent(KIND_FAILURE,referrer);
    }

    private DeeplinkReportEvent(@NotNull String kind, String referrer) {
        super(kind);
        this.referrer = referrer;
    }

    @Override
    public Metric toMetric() {
        return Metric.create("DeeplinksReport", DataPoint.string(referrer, getKind()));
    }

}
