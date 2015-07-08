package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
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
    public static final String PLAYER_TYPE = "player_type";
    public static final String CONNECTION_TYPE = "connection_type";

    private static final String EVENT_KIND_PLAY = "play";
    private static final String EVENT_KIND_STOP = "stop";

    private static final String MONETIZATION_AUDIO_AD = "audio_ad";
    private static final String MONETIZATION_PROMOTED = "promoted";

    public static final long FIRST_PLAY_MAX_PROGRESS = 1000L;

    private final Urn trackUrn;
    private final long duration;
    private final long progress;

    private int stopReason;
    private long listenTime;
    private final TrackSourceInfo trackSourceInfo;

    private List<String> adCompanionImpressionUrls = Collections.emptyList();
    private List<String> adImpressionUrls = Collections.emptyList();
    private List<String> adFinishedUrls = Collections.emptyList();
    private List<String> promotedPlayUrls = Collections.emptyList();

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn, TrackSourceInfo trackSourceInfo, long progress,
                                               String protocol, String playerType, String connectionType) {
        return forPlay(trackData, userUrn, trackSourceInfo, progress, System.currentTimeMillis(), protocol, playerType, connectionType);
    }

    @VisibleForTesting
    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn, TrackSourceInfo trackSourceInfo, long progress, long timestamp,
                                               String protocol, String playerType, String connectionType) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackData, userUrn, trackSourceInfo, progress, timestamp, protocol, playerType, connectionType);
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent, long progress,
                                               String protocol, String playerType, String connectionType, int stopReason) {
        return forStop(trackData, userUrn, trackSourceInfo, lastPlayEvent, progress, System.currentTimeMillis(), protocol, playerType, connectionType, stopReason);
    }

    @VisibleForTesting
    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent, long progress, long timestamp,
                                               String protocol, String playerType, String connectionType, int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackData, userUrn, trackSourceInfo, progress, timestamp, protocol, playerType, connectionType);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timestamp - lastPlayEvent.getTimestamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    // Regular track
    private PlaybackSessionEvent(String eventKind, PropertySet track, Urn userUrn, TrackSourceInfo trackSourceInfo, long progress, long timestamp,
                                 String protocol, String playerType, String connectionType) {
        super(eventKind, timestamp);
        this.trackUrn = track.get(TrackProperty.URN);
        put(KEY_TRACK_URN, trackUrn.toString());
        put(KEY_USER_URN, userUrn.toString());
        put(KEY_PROTOCOL, protocol);
        put(KEY_POLICY, track.getOrElseNull(TrackProperty.POLICY));
        put(PLAYER_TYPE, playerType);
        put(CONNECTION_TYPE, connectionType);
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.duration = track.get(PlayableProperty.DURATION);
    }

    // Audio ad
    public PlaybackSessionEvent withAudioAd(PropertySet audioAd) {
        put(AdTrackingKeys.KEY_USER_URN, get(KEY_USER_URN));
        put(AdTrackingKeys.KEY_AD_URN, audioAd.get(AdProperty.AUDIO_AD_URN));
        put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_AUDIO_AD);
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

    // Promoted track
    public PlaybackSessionEvent withPromotedTrack(PromotedSourceInfo promotedSource) {
        put(AdTrackingKeys.KEY_AD_URN, promotedSource.getAdUrn());
        put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_PROMOTED);
        if (promotedSource.getPromoterUrn().isPresent()) {
            put(AdTrackingKeys.KEY_PROMOTER_URN, promotedSource.getPromoterUrn().get().toString());
        }
        this.promotedPlayUrls = promotedSource.getTrackingUrls();
        return this;
    }

    public boolean isPlayEvent() {
        return EVENT_KIND_PLAY.equals(kind);
    }

    public boolean isBufferingEvent() {
        return stopReason == STOP_REASON_BUFFERING;
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

    public long getDuration() {
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

    public List<String> getPromotedPlayUrls() {
        return promotedPlayUrls;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    private void setListenTime(long listenTime) {
        this.listenTime = listenTime;
    }

    public int getStopReason() {
        return stopReason;
    }

    private void setStopReason(int stopReason) {
        this.stopReason = stopReason;
    }

    public long getListenTime() {
        return listenTime;
    }

    public boolean isAd() {
        return isMonetizationType(MONETIZATION_AUDIO_AD);
    }

    public boolean isPromotedTrack() {
        return isMonetizationType(MONETIZATION_PROMOTED);
    }

    private boolean isMonetizationType(String type) {
        return attributes.containsKey(AdTrackingKeys.KEY_MONETIZATION_TYPE)
                && attributes.get(AdTrackingKeys.KEY_MONETIZATION_TYPE).equals(type);
    }

    public boolean isFirstPlay() {
        return isPlayEvent() && 0L <= progress && progress <= FIRST_PLAY_MAX_PROGRESS;
    }

    public boolean hasTrackFinished() {
        return isStopEvent() && getStopReason() == PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED;
    }
}
