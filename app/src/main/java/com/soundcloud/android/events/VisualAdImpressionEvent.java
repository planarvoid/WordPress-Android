package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

import java.util.List;

public class VisualAdImpressionEvent extends TrackingEvent {
    public static final String KEY_USER_URN = "user_urn";
    public static final String KEY_AD_TRACK_URN = "ad_track_urn";
    public static final String KEY_MONETIZABLE_TRACK_URN = "monetizable_track_urn";
    public static final String KEY_AD_URN = "ad_urn";
    public static final String KEY_AD_ARTWORK_URL = "ad_artwork_url";
    private final List<String> impressionUrls;

    public VisualAdImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, Urn userUrn) {
        this(adMetaData, audioAdTrack, userUrn, System.currentTimeMillis());
    }

    public VisualAdImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, Urn userUrn, long timeStamp) {
        super(KIND_DEFAULT, timeStamp);
        put(KEY_USER_URN, userUrn.toString());
        put(KEY_AD_TRACK_URN, audioAdTrack.toString());
        put(KEY_MONETIZABLE_TRACK_URN, adMetaData.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
        put(KEY_AD_URN, adMetaData.get(AdProperty.AD_URN));
        put(KEY_AD_ARTWORK_URL, adMetaData.get(AdProperty.ARTWORK).toString());
        this.impressionUrls = adMetaData.get(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS);
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }
}
