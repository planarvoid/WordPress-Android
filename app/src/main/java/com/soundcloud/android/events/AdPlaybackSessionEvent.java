package com.soundcloud.android.events;

import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdPlaybackSessionEvent extends TrackingEvent {

    public static final String EVENT_KIND_PLAY = "play";
    public static final String EVENT_KIND_STOP = "stop";
    public static final String EVENT_KIND_QUARTILE = "quartile_event";

    private static final String FIRST_QUARTILE_TYPE = "ad::first_quartile";
    private static final String SECOND_QUARTILE_TYPE = "ad::second_quartile";
    private static final String THIRD_QUARTILE_TYPE = "ad::third_quartile";

    private static final String MONETIZATION_AUDIO = "audio_ad";
    private static final String MONETIZATION_VIDEO = "video_ad";

    public static final long FIRST_PLAY_MAX_PROGRESS = 1000L;

    public final TrackSourceInfo trackSourceInfo;

    private int stopReason;
    private PlaybackStateTransition stateTransition;
    private List<String> trackingUrls = Collections.emptyList();

    public static AdPlaybackSessionEvent forFirstQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return forQuartile(adData, trackSourceInfo, FIRST_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forSecondQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return forQuartile(adData, trackSourceInfo, SECOND_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forThirdQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return forQuartile(adData, trackSourceInfo, THIRD_QUARTILE_TYPE);
    }

    private static AdPlaybackSessionEvent forQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo,
                                                      String quartileType)  {
        return new AdPlaybackSessionEvent(EVENT_KIND_QUARTILE, adData, trackSourceInfo)
                .setQuartileTrackingUrls(quartileType, adData)
                .put(PlayableTrackingKeys.KEY_QUARTILE_TYPE, quartileType);
    }

    public static AdPlaybackSessionEvent forPlay(PlayerAdData adData, TrackSourceInfo trackSourceInfo,
                                                 PlaybackStateTransition stateTransition) {
        return new AdPlaybackSessionEvent(EVENT_KIND_PLAY, adData, trackSourceInfo)
                .setStateTransition(stateTransition)
                .setPlaybackTrackingUrls(adData);
    }

    public static AdPlaybackSessionEvent forStop(PlayerAdData adData, TrackSourceInfo trackSourceInfo,
                                                 PlaybackStateTransition stateTransition, int stopReason) {
        return new AdPlaybackSessionEvent(EVENT_KIND_STOP, adData, trackSourceInfo)
                .setStateTransition(stateTransition)
                .setStopReason(stopReason)
                .setPlaybackTrackingUrls(adData);
    }

    private AdPlaybackSessionEvent(String kind, PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        super(kind, System.currentTimeMillis());
        this.trackSourceInfo = trackSourceInfo;

        put(PlayableTrackingKeys.KEY_AD_URN, adData.getAdUrn().toString());
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString());
        put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, adData instanceof VideoAd ? MONETIZATION_VIDEO : MONETIZATION_AUDIO);
    }

    public boolean isFirstPlay() {
        if (isKind(EVENT_KIND_PLAY)) {
            final PlaybackProgress progress = this.stateTransition.getProgress();
            return 0L <= progress.getPosition() && progress.getPosition() <= FIRST_PLAY_MAX_PROGRESS;
        }
        return false;
    }

    public boolean hasAdFinished() {
        return isKind(EVENT_KIND_STOP) && this.stopReason == PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED;
    }

    private boolean wasAdPaused() {
        return isKind(EVENT_KIND_STOP) && this.stopReason == PlaybackSessionEvent.STOP_REASON_PAUSE;
    }

    private boolean isKind(String kind) {
        return this.kind.equals(kind);
    }

    public AdPlaybackSessionEvent setStopReason(int stopReason) {
        this.stopReason = stopReason;
        return this;
    }

    public AdPlaybackSessionEvent setStateTransition(PlaybackStateTransition stateTransition) {
        this.stateTransition = stateTransition;
        return this;
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    private AdPlaybackSessionEvent setQuartileTrackingUrls(String quartileType, PlayerAdData adData) {
        switch (quartileType) {
            case FIRST_QUARTILE_TYPE:
                trackingUrls = adData.getFirstQuartileUrls();
                break;
            case SECOND_QUARTILE_TYPE:
                trackingUrls = adData.getSecondQuartileUrls();
                break;
            case THIRD_QUARTILE_TYPE:
                trackingUrls = adData.getThirdQuartileUrls();
                break;
        }
        return this;
    }

    private AdPlaybackSessionEvent setPlaybackTrackingUrls(PlayerAdData adData) {
        if (isKind(EVENT_KIND_PLAY)) {
            setPlayEventTrackingUrls(adData);
        } else {
            setStopEventTrackingUrls(adData);
        }
        return this;
    }

    private void setPlayEventTrackingUrls(PlayerAdData adData) {
        if (isFirstPlay()) {
            trackingUrls = new ArrayList<>();
            trackingUrls.addAll(adData.getImpressionUrls());
            trackingUrls.addAll(adData.getStartUrls());
        } else {
            trackingUrls = adData.getResumeUrls();
        }
    }

    private void setStopEventTrackingUrls(PlayerAdData adData) {
        if (hasAdFinished()) {
            trackingUrls = adData.getFinishUrls();
        } else if (wasAdPaused()) {
            trackingUrls = adData.getPauseUrls();
        }
    }

}
