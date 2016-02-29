package com.soundcloud.android.ads;

import java.util.List;

public abstract class PlayerAdData extends AdData {
    private boolean firstQuartileReached;
    private boolean secondQuartileReached;
    private boolean thirdQuartileReached;

    public boolean hasReachedFirstQuartile() {
        return firstQuartileReached;
    }

    public boolean hasReachedSecondQuartile() {
        return secondQuartileReached;
    }

    public boolean hasReachedThirdQuartile() {
        return thirdQuartileReached;
    }

    public void setFirstQuartileReached() {
        firstQuartileReached = true;
    }

    public void setSecondQuartileReached() {
        secondQuartileReached = true;
    }

    public void setThirdQuartileReached() {
        thirdQuartileReached = true;
    }

    public abstract CompanionAd getVisualAd();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getFinishUrls();

    public abstract List<String> getSkipUrls();

}
