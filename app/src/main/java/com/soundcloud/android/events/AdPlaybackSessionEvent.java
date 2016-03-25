package com.soundcloud.android.events;

import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.TrackSourceInfo;

import java.util.Collections;
import java.util.List;

public class AdPlaybackSessionEvent extends TrackingEvent {

    private static final String EVENT_KIND_QUARTILE = "quartile_event";

    private static final String FIRST_QUARTILE_TYPE = "ad::first_quartile";
    private static final String SECOND_QUARTILE_TYPE = "ad::second_quartile";
    private static final String THIRD_QUARTILE_TYPE = "ad::third_quartile";

    private static final String MONETIZATION_AUDIO = "audio_ad";
    private static final String MONETIZATION_VIDEO = "video_ad";

    public final TrackSourceInfo trackSourceInfo;

    private List<String> quartileTrackingUrls = Collections.emptyList();

    public static AdPlaybackSessionEvent forFirstQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(FIRST_QUARTILE_TYPE, adData, trackSourceInfo);
    }

    public static AdPlaybackSessionEvent forSecondQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(SECOND_QUARTILE_TYPE, adData, trackSourceInfo);
    }

    public static AdPlaybackSessionEvent forThirdQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(THIRD_QUARTILE_TYPE, adData, trackSourceInfo);
    }

    private AdPlaybackSessionEvent(String quartileType, PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        super(EVENT_KIND_QUARTILE, System.currentTimeMillis());

        this.trackSourceInfo = trackSourceInfo;

        put(AdTrackingKeys.KEY_QUARTILE_TYPE, quartileType);
        put(AdTrackingKeys.KEY_AD_URN, adData.getAdUrn().toString());
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString());
        if (adData instanceof VideoAd) {
            setQuartileTrackingUrls(quartileType, (VideoAd) adData);
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_VIDEO);
        } else {
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_AUDIO);
        }
    }

    public List<String> getQuartileTrackingUrls() {
        return quartileTrackingUrls;
    }

    private void setQuartileTrackingUrls(String quartileType, VideoAd videoData) {
        switch (quartileType) {
            case FIRST_QUARTILE_TYPE:
                quartileTrackingUrls = videoData.getFirstQuartileUrls();
                break;
            case SECOND_QUARTILE_TYPE:
                quartileTrackingUrls = videoData.getSecondQuartileUrls();
                break;
            case THIRD_QUARTILE_TYPE:
                quartileTrackingUrls = videoData.getThirdQuartileUrls();
                break;
        }
    }

}
