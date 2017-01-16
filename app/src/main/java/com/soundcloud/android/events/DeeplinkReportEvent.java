package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class DeeplinkReportEvent extends NewTrackingEvent implements MetricEvent {

    enum Kind {
        SUCCESS("Success"),
        FAILURE("Failed");

        private final String key;
        Kind(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public abstract Kind kind();

    public abstract String referrer();

    public static DeeplinkReportEvent forResolvedDeeplink(String referrer) {
        return new AutoValue_DeeplinkReportEvent(defaultId(), defaultTimestamp(), Optional.absent(), Kind.SUCCESS, referrer);
    }

    public static DeeplinkReportEvent forResolutionFailure(String referrer) {
        return new AutoValue_DeeplinkReportEvent(defaultId(), defaultTimestamp(), Optional.absent(), Kind.FAILURE, referrer);
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_DeeplinkReportEvent(id(), timestamp(), Optional.of(referringEvent), kind(), referrer());
    }

    @Override
    public Metric toMetric() {
        return Metric.create("DeeplinksReport", DataPoint.string(referrer(), kind().toString()));
    }

}
