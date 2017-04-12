package com.soundcloud.android.ads;

import com.soundcloud.java.optional.Optional;

import java.util.List;

public abstract class PlayableAdData extends AdData {

    public enum ReportingEvent {
        START,
        FIRST_QUARTILE,
        SECOND_QUARTILE,
        THIRD_QUARTILE,
        FINISH
    }

    private boolean[] eventsReported = new boolean[ReportingEvent.values().length];

    public boolean hasReportedEvent(ReportingEvent event) {
        return eventsReported[event.ordinal()];
    }

    public void setEventReported(ReportingEvent event) {
        eventsReported[event.ordinal()] = true;
    }

    public abstract Optional<String> callToActionButtonText();

    public abstract List<String> impressionUrls();
    public abstract List<String> startUrls();
    public abstract List<String> finishUrls();
    public abstract List<String> skipUrls();
    public abstract List<String> firstQuartileUrls();
    public abstract List<String> secondQuartileUrls();
    public abstract List<String> thirdQuartileUrls();
    public abstract List<String> pauseUrls();
    public abstract List<String> resumeUrls();
    public abstract List<String> clickUrls();

    public abstract boolean isSkippable();

    public abstract Optional<VisualAdDisplayProperties> displayProperties();
}
