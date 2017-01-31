package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class InlayAdImpressionEvent extends NewTrackingEvent {
    public static final String eventName = "impression";
    public static final String impressionName = "app_install";
    public static final String pageName = Screen.STREAM.get();
    public static final String monetizationType = "mobile_inlay";

    public abstract Urn ad();

    public abstract int contextPosition();

    public abstract List<String> impressionUrls();

    public static InlayAdImpressionEvent create(AppInstallAd adData,
                                                int position,
                                                long timeStamp) {
        return new AutoValue_InlayAdImpressionEvent(defaultId(), timeStamp, Optional.absent(), adData.getAdUrn(), position, adData.getImpressionUrls());
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_InlayAdImpressionEvent(id(), timestamp(), Optional.of(referringEvent), ad(), contextPosition(), impressionUrls());
    }
}
