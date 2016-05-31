package com.soundcloud.android.events;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public class VisualAdImpressionEvent extends TrackingEvent {
    private List<String> impressionUrls;

    public VisualAdImpressionEvent(AudioAd adData, Urn audioAdTrack, Urn userUrn, TrackSourceInfo sessionSource) {
        this(adData, audioAdTrack, userUrn, sessionSource, System.currentTimeMillis());
    }

    @VisibleForTesting
    public VisualAdImpressionEvent(AudioAd adData, Urn audioAdTrack, Urn userUrn, TrackSourceInfo sessionSource, long timeStamp) {
        super(KIND_DEFAULT, timeStamp);
        put(PlayableTrackingKeys.KEY_USER_URN, userUrn.toString());
        put(PlayableTrackingKeys.KEY_AD_TRACK_URN, audioAdTrack.toString());
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString());
        put(PlayableTrackingKeys.KEY_AD_URN, adData.getVisualAd().getAdUrn().toString());
        put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, adData.getVisualAd().getImageUrl().toString());
        put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, sessionSource.getOriginScreen());
        this.impressionUrls = adData.getVisualAd().getImpressionUrls();
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }
}
