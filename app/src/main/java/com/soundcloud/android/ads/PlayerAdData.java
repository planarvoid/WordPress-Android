package com.soundcloud.android.ads;

import java.util.List;

public abstract class PlayerAdData extends AdData {
    private boolean firstQuartileReported;
    private boolean secondQuartileReported;
    private boolean thirdQuartileReported;

    public boolean hasReportedFirstQuartile() {
        return firstQuartileReported;
    }

    public boolean hasReportedSecondQuartile() {
        return secondQuartileReported;
    }

    public boolean hasReportedThirdQuartile() {
        return thirdQuartileReported;
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

    public abstract CompanionAd getVisualAd();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getFinishUrls();

    public abstract List<String> getSkipUrls();

}
