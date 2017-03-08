package com.soundcloud.android.ads;

import com.soundcloud.java.optional.Optional;

import java.util.List;

public abstract class PlayableAdData extends AdData {

    public enum ReportingEvent {
        START_EVENT,
        FIRST_QUARTILE,
        SECOND_QUARTILE,
        THIRD_QUARTILE
    }

    public enum MonetizationType {
        MONETIZATION_AUDIO("audio_ad"),
        MONETIZATION_VIDEO("video_ad"),
        MONETIZATION_INLAY("mobile_inlay");

        private final String key;

        MonetizationType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private boolean[] eventsReported = new boolean[ReportingEvent.values().length];

    public boolean hasReportedEvent(ReportingEvent event) {
        return eventsReported[event.ordinal()];
    }

    public void setEventReported(ReportingEvent event) {
        eventsReported[event.ordinal()] = true;
    }

    public abstract Optional<String> getCallToActionButtonText();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getStartUrls();

    public abstract List<String> getFinishUrls();

    public abstract List<String> getSkipUrls();

    public abstract List<String> getFirstQuartileUrls();

    public abstract List<String> getSecondQuartileUrls();

    public abstract List<String> getThirdQuartileUrls();

    public abstract List<String> getPauseUrls();

    public abstract List<String> getResumeUrls();

    public abstract List<String> getClickUrls();

    public abstract MonetizationType getMonetizationType();

    public abstract boolean isSkippable();

    public abstract Optional<VisualAdDisplayProperties> getDisplayProperties();


}
