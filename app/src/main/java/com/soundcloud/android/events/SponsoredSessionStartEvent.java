package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.SponsoredSessionAd;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class SponsoredSessionStartEvent extends TrackingEvent {

    public static final String CLICK_NAME = "ad::start_session";

    public abstract Urn adUrn();
    public abstract String clickTarget();
    public abstract Screen page();
    public abstract String monetizationType();

    public abstract List<String> trackingUrls();

    public static SponsoredSessionStartEvent create(SponsoredSessionAd ad, Screen page) {
        return new AutoValue_SponsoredSessionStartEvent(defaultId(),
                                                        defaultTimestamp(),
                                                        Optional.absent(),
                                                        ad.adUrn(),
                                                        String.valueOf(ad.adFreeLength()),
                                                        page,
                                                        ad.monetizationType().key(),
                                                        ad.rewardUrls());
    }

    @Override
    public SponsoredSessionStartEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_SponsoredSessionStartEvent(id(),
                                                        timestamp(),
                                                        Optional.of(referringEvent),
                                                        adUrn(),
                                                        clickTarget(),
                                                        page(),
                                                        monetizationType(),
                                                        trackingUrls());
    }
}
