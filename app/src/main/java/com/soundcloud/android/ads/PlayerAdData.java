package com.soundcloud.android.ads;

import java.util.List;

public abstract class PlayerAdData extends AdData {

    public abstract CompanionAd getVisualAd();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getFinishUrls();

    public abstract List<String> getSkipUrls();

}
