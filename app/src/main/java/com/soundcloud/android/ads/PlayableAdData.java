package com.soundcloud.android.ads;

import com.soundcloud.java.optional.Optional;

import java.util.List;

public abstract class PlayableAdData extends AdData {

    private boolean startEventsReported;
    private boolean firstQuartileReported;
    private boolean secondQuartileReported;
    private boolean thirdQuartileReported;

    public boolean hasReportedStart() {
        return startEventsReported;
    }

    public boolean hasReportedFirstQuartile() {
        return firstQuartileReported;
    }

    public boolean hasReportedSecondQuartile() {
        return secondQuartileReported;
    }

    public boolean hasReportedThirdQuartile() {
        return thirdQuartileReported;
    }

    public void setStartReported() {
        startEventsReported = true;
    }

    public void setFirstQuartileReported() {
        firstQuartileReported = true;
    }

    public void setSecondQuartileReported() {
        secondQuartileReported = true;
    }

    public void setThirdQuartileReported() {
        thirdQuartileReported = true;
    }

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

    public abstract boolean isSkippable();

    public abstract Optional<VisualAdDisplayProperties> getDisplayProperties();

}
