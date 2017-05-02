package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.VisualPrestitialAd;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class PrestitialAdImpressionEvent extends TrackingEvent {
    public static final String EVENT_NAME = "impression";
    public static final String IMPRESSION_NAME = "display";
    public static final String PAGE_NAME = Screen.VISUAL_PRESTITIAL.get();

    public abstract Urn urn();

    public abstract List<String> impressionUrls();

    public abstract String monetizationType();

    public static PrestitialAdImpressionEvent create(VisualPrestitialAd adData) {
        return new AutoValue_PrestitialAdImpressionEvent(defaultId(), defaultTimestamp(), Optional.absent(), adData.adUrn(), adData.impressionUrls(), adData.monetizationType().key());
    }

    @Override
    public PrestitialAdImpressionEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PrestitialAdImpressionEvent(id(), timestamp(), Optional.of(referringEvent), urn(), impressionUrls(), monetizationType());
    }
}
