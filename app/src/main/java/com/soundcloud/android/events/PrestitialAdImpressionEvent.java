package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.SponsoredSessionAd;
import com.soundcloud.android.ads.VisualPrestitialAd;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class PrestitialAdImpressionEvent extends TrackingEvent {
    private static final String DISPLAY_IMPRESSION_NAME = "display";
    private static final String END_CARD_IMPRESSION_NAME = "end_card";

    public static final String EVENT_NAME = "impression";
    public static final String PAGE_NAME = Screen.PRESTITIAL.get();

    public abstract Urn urn();

    public abstract String impressionName();

    public abstract List<String> impressionUrls();

    public abstract String monetizationType();

    private static PrestitialAdImpressionEvent create(Urn urn, String impressionName, List<String> impressionUrls, String monetizationType) {
        return new AutoValue_PrestitialAdImpressionEvent(defaultId(),
                                                         defaultTimestamp(),
                                                         Optional.absent(),
                                                         urn,
                                                         impressionName,
                                                         impressionUrls,
                                                         monetizationType);
    }

    public static PrestitialAdImpressionEvent createForSponsoredSession(SponsoredSessionAd ad, boolean isEndCard) {
        final SponsoredSessionAd.OptInCard displayCard = ad.optInCard();
        final String impressionName = isEndCard ? END_CARD_IMPRESSION_NAME : DISPLAY_IMPRESSION_NAME;
        final List<String> impressionUrls = isEndCard ? Collections.emptyList() : displayCard.trackingImpressionUrls();
        return create(displayCard.adUrn(), impressionName, impressionUrls, ad.monetizationType().key());
    }

    public static PrestitialAdImpressionEvent createForDisplay(VisualPrestitialAd ad) {
        return create(ad.adUrn(), DISPLAY_IMPRESSION_NAME, ad.impressionUrls(), ad.monetizationType().key());
    }

    @Override
    public PrestitialAdImpressionEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PrestitialAdImpressionEvent(id(),
                                                         timestamp(),
                                                         Optional.of(referringEvent),
                                                         urn(),
                                                         impressionName(),
                                                         impressionUrls(),
                                                         monetizationType());
    }
}
