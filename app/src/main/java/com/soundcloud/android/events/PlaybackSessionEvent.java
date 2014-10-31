package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackSessionEvent extends TrackingEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    public static final String KEY_TRACK_URN = "track_urn";
    public static final String KEY_USER_URN = "user_urn";
    public static final String KEY_PROTOCOL = "protocol";
    public static final String KEY_POLICY = "policy";

    private static final String EVENT_KIND_PLAY = "play";
    private static final String EVENT_KIND_STOP = "stop";

    private final int duration;
    private final long progress;

    private int stopReason;
    private final TrackSourceInfo trackSourceInfo;
    private List<String> adCompanionImpressionUrls = Collections.emptyList();
    private List<String> adImpressionUrls = Collections.emptyList();
    private List<String> adFinishedUrls = Collections.emptyList();

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress) {
        return forPlay(trackData, userUrn, protocol, trackSourceInfo, progress, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo,
                                               int stopReason, long progress, long timestamp) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo,
                                               int stopReason, long progress) {
        return forStop(trackData, userUrn, protocol, trackSourceInfo, stopReason, progress, System.currentTimeMillis());
    }

    // Use this constructor for an ordinary audio playback event
    private PlaybackSessionEvent(String eventKind, PropertySet track, Urn userUrn,
                                 String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        super(eventKind, timestamp);
        put(KEY_TRACK_URN, track.get(TrackProperty.URN).toString());
        put(KEY_USER_URN, userUrn.toString());
        put(KEY_PROTOCOL, protocol);
        put(KEY_POLICY, track.getOrElseNull(TrackProperty.POLICY));
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.duration = track.get(PlayableProperty.DURATION);
    }

    // Use this constructor for an audio ad playback event
    public PlaybackSessionEvent withAudioAd(PropertySet audioAd) {
        put(AdTrackingKeys.KEY_USER_URN, get(KEY_USER_URN));
        put(AdTrackingKeys.KEY_AD_URN, audioAd.get(AdProperty.AD_URN));
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
        put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.get(AdProperty.ARTWORK).toString());
        put(AdTrackingKeys.KEY_AD_TRACK_URN, get(KEY_TRACK_URN));
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, trackSourceInfo.getOriginScreen());
        put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, get(KEY_TRACK_URN));
        this.adImpressionUrls = audioAd.get(AdProperty.AUDIO_AD_IMPRESSION_URLS);
        this.adCompanionImpressionUrls = audioAd.get(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS);
        this.adFinishedUrls = audioAd.get(AdProperty.AUDIO_AD_FINISH_URLS);
        return this;
    }

    public boolean isPlayEvent() {
        return EVENT_KIND_PLAY.equals(kind);
    }

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
    }

    public boolean isPlayingOwnPlaylist() {
        return trackSourceInfo.getPlaylistOwnerUrn().toString().equals(get(KEY_USER_URN));
    }

    public long getProgress() {
        return progress;
    }

    public int getDuration() {
        return duration;
    }

    public List<String> getAudioAdImpressionUrls() {
        return adImpressionUrls;
    }

    public List<String> getAudioAdFinishUrls() {
        return adFinishedUrls;
    }

    public List<String> getAudioAdCompanionImpressionUrls() {
        return adCompanionImpressionUrls;
    }

    public int getStopReason() {
        return stopReason;
    }

    private void setStopReason(int stopReason) {
        this.stopReason = stopReason;
    }

    public boolean isAd() {
        return attributes.containsKey(AdTrackingKeys.KEY_AD_URN);
    }

    public boolean isFirstPlay() {
        return isPlayEvent() && progress == 0L;
    }

    public boolean hasTrackFinished() {
        return isStopEvent() && getStopReason() == PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED;
    }
}
