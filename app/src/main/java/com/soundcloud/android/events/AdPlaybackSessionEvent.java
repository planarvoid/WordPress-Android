package com.soundcloud.android.events;

import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player.StateTransition;
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
    private StateTransition stateTransition;
    private List<String> trackingUrls = Collections.emptyList();

    public static AdPlaybackSessionEvent forFirstQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(EVENT_KIND_QUARTILE, adData, trackSourceInfo)
                .setQuartileTrackingUrls(FIRST_QUARTILE_TYPE, adData)
                .put(AdTrackingKeys.KEY_QUARTILE_TYPE, FIRST_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forSecondQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(EVENT_KIND_QUARTILE, adData, trackSourceInfo)
                .setQuartileTrackingUrls(SECOND_QUARTILE_TYPE, adData)
                .put(AdTrackingKeys.KEY_QUARTILE_TYPE, SECOND_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forThirdQuartile(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdPlaybackSessionEvent(EVENT_KIND_QUARTILE, adData, trackSourceInfo)
                .setQuartileTrackingUrls(THIRD_QUARTILE_TYPE, adData)
                .put(AdTrackingKeys.KEY_QUARTILE_TYPE, THIRD_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forPlay(PlayerAdData adData, TrackSourceInfo trackSourceInfo, StateTransition stateTransition) {
        return new AdPlaybackSessionEvent(EVENT_KIND_PLAY, adData, trackSourceInfo)
                .setStateTransition(stateTransition)
                .setPlaybackTrackingUrls(adData);
    }

    public static AdPlaybackSessionEvent forStop(PlayerAdData adData, TrackSourceInfo trackSourceInfo, StateTransition stateTransition, int stopReason) {
        return new AdPlaybackSessionEvent(EVENT_KIND_STOP, adData, trackSourceInfo)
                .setStateTransition(stateTransition)
                .setStopReason(stopReason)
                .setPlaybackTrackingUrls(adData);
    }

    private AdPlaybackSessionEvent(String kind, PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        super(kind, System.currentTimeMillis());

        this.trackSourceInfo = trackSourceInfo;

        put(AdTrackingKeys.KEY_AD_URN, adData.getAdUrn().toString());
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, adData.getMonetizableTrackUrn().toString());
        if (adData instanceof VideoAd) {
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_VIDEO);
        } else {
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_AUDIO);
        }
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

    public AdPlaybackSessionEvent setStateTransition(StateTransition stateTransition) {
        this.stateTransition = stateTransition;
        return this;
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    private AdPlaybackSessionEvent setQuartileTrackingUrls(String quartileType, PlayerAdData adData) {
        if (adData instanceof VideoAd) {
            final VideoAd videoData = (VideoAd) adData;
            switch (quartileType) {
                case FIRST_QUARTILE_TYPE:
                    trackingUrls = videoData.getFirstQuartileUrls();
                    break;
                case SECOND_QUARTILE_TYPE:
                    trackingUrls = videoData.getSecondQuartileUrls();
                    break;
                case THIRD_QUARTILE_TYPE:
                    trackingUrls = videoData.getThirdQuartileUrls();
                    break;
            }
        }
        return this;
    }

    private AdPlaybackSessionEvent setPlaybackTrackingUrls(PlayerAdData adData) {
        if (adData instanceof VideoAd) {
            final VideoAd videoData = (VideoAd) adData;
            if (isKind(EVENT_KIND_PLAY)) {
                setPlayEventTrackingUrls(videoData);
            } else {
                setStopEventTrackingUrls(videoData);
            }
        }
        return this;
    }

    private void setPlayEventTrackingUrls(VideoAd videoData) {
        if (isFirstPlay()) {
            trackingUrls = new ArrayList<>();
            trackingUrls.addAll(videoData.getImpressionUrls());
            trackingUrls.addAll(videoData.getStartUrls());
        } else {
            trackingUrls = videoData.getResumeUrls();
        }
    }

    private void setStopEventTrackingUrls(VideoAd videoData) {
        if (hasAdFinished()) {
            trackingUrls = videoData.getFinishUrls();
        } else if (wasAdPaused()) {
            trackingUrls = videoData.getPauseUrls();
        }
    }

}
