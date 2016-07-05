package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;
import org.jetbrains.annotations.NotNull;

public class DeeplinkReportEvent extends TrackingEvent implements MetricEvent {

    private static final String DEEPLINK_STATUS = "DeeplinkStatus";

    private static final String KIND_SUCCESS = "Success";
    private static final String KIND_FAILURE = "Failed";

    public static DeeplinkReportEvent forResolvedDeeplink() {
        return new DeeplinkReportEvent(KIND_SUCCESS);
    }

    public static DeeplinkReportEvent forResolutionFailure() {
        return new DeeplinkReportEvent(KIND_FAILURE);
    }

    private DeeplinkReportEvent(@NotNull String kind) {
        super(kind, System.currentTimeMillis());
    }

    @Override
    public Metric toMetric() {
        return Metric.create("DeeplinksReport", DataPoint.string(DEEPLINK_STATUS, getKind()));
    }

}
