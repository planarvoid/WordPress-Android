package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

public class VisualAdImpressionEvent extends TrackingEvent {
    private final List<String> impressionUrls;

    public VisualAdImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, Urn userUrn, TrackSourceInfo sessionSource) {
        this(adMetaData, audioAdTrack, userUrn, sessionSource, System.currentTimeMillis());
    }

    @VisibleForTesting
    public VisualAdImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, Urn userUrn, TrackSourceInfo sessionSource, long timeStamp) {
        super(KIND_DEFAULT, timeStamp);
        put(AdTrackingKeys.KEY_USER_URN, userUrn.toString());
        put(AdTrackingKeys.KEY_AD_TRACK_URN, audioAdTrack.toString());
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adMetaData.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
        put(AdTrackingKeys.KEY_AD_URN, adMetaData.get(AdProperty.COMPANION_URN));
        put(AdTrackingKeys.KEY_AD_ARTWORK_URL, adMetaData.get(AdProperty.ARTWORK).toString());
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, sessionSource.getOriginScreen());
        this.impressionUrls = adMetaData.get(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS);
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }
}
