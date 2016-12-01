package com.soundcloud.android.events;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public class VisualAdImpressionEvent extends LegacyTrackingEvent {
    private List<String> impressionUrls;

    public VisualAdImpressionEvent(AudioAd adData, Urn userUrn, TrackSourceInfo sessionSource) {
        this(adData, userUrn, sessionSource, System.currentTimeMillis());
    }

    @VisibleForTesting
    public VisualAdImpressionEvent(AudioAd adData,
                                   Urn userUrn,
                                   TrackSourceInfo sessionSource,
                                   long timeStamp) {
        super(KIND_DEFAULT, timeStamp);
        put(PlayableTrackingKeys.KEY_USER_URN, userUrn.toString());
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString());
        put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, sessionSource.getOriginScreen());
        put(PlayableTrackingKeys.KEY_AD_URN, adData.getCompanionAdUrn());
        put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, adData.getCompanionImageUrl());
        this.impressionUrls = adData.getCompanionImpressionUrls();
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }
}
