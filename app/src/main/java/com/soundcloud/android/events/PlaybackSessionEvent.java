package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackProperty;

import android.support.annotation.NonNull;

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
    public static final int STOP_REASON_CONCURRENT_STREAMING = 7;

    public static final String KEY_LOGGED_IN_USER_URN = "logged_in_user_urn";
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
    private final Urn creatorUrn;
    private final long duration;
    private final long progress;
    private final boolean isOfflineTrack;
    private final boolean marketablePlay;
    private final String uuid;
    private final String monetizationModel;

    private int stopReason;
    private long listenTime;
    private final TrackSourceInfo trackSourceInfo;

    private List<String> adCompanionImpressionUrls = Collections.emptyList();
    private List<String> adImpressionUrls = Collections.emptyList();
    private List<String> adFinishedUrls = Collections.emptyList();
    private List<String> promotedPlayUrls = Collections.emptyList();

    public static PlaybackSessionEvent forPlay(PlaybackSessionEventArgs args) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, args);
    }

    @NonNull
    public static PlaybackSessionEvent forStop(PlaybackSessionEvent lastPlayEvent, int stopReason, PlaybackSessionEventArgs args) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, args);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timestamp - lastPlayEvent.getTimestamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    // Regular track
    private PlaybackSessionEvent(String eventKind, PlaybackSessionEventArgs args) {
        super(eventKind, args.getDateProvider().getCurrentTime());
        this.isOfflineTrack = args.isOfflineTrack();
        this.marketablePlay = args.isMarketablePlay();
        this.uuid = args.getUuid();
        this.trackUrn = args.getTrackData().get(TrackProperty.URN);
        this.creatorUrn = args.getTrackData().get(TrackProperty.CREATOR_URN);
        this.monetizationModel = args.getTrackData().get(TrackProperty.MONETIZATION_MODEL);
        put(KEY_LOGGED_IN_USER_URN, args.getUserUrn().toString());
        put(KEY_PROTOCOL, args.getProtocol());
        put(KEY_POLICY, args.getTrackData().getOrElseNull(TrackProperty.POLICY));
        put(PLAYER_TYPE, args.getPlayerType());
        put(CONNECTION_TYPE, args.getConnectionType());
        this.trackSourceInfo = args.getTrackSourceInfo();
        this.progress = args.getProgress();
        this.duration = Durations.getTrackPlayDuration(args.getTrackData());
        EntityMetadata.from(args.getTrackData()).addToTrackingEvent(this);
    }

    // Audio ad
    public PlaybackSessionEvent withAudioAd(AudioAd audioAd) {
        put(AdTrackingKeys.KEY_USER_URN, get(KEY_LOGGED_IN_USER_URN));
        put(AdTrackingKeys.KEY_AD_URN, audioAd.getAdUrn().toString());
        put(AdTrackingKeys.KEY_MONETIZATION_TYPE, MONETIZATION_AUDIO_AD);
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, audioAd.getMonetizableTrackUrn().toString());
        put(AdTrackingKeys.KEY_AD_ARTWORK_URL, audioAd.getVisualAd().getImageUrl().toString());
        put(AdTrackingKeys.KEY_AD_TRACK_URN, trackUrn.toString());
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, trackSourceInfo.getOriginScreen());
        put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, trackUrn.toString());
        this.adImpressionUrls = audioAd.getImpressionUrls();
        this.adCompanionImpressionUrls = audioAd.getVisualAd().getImpressionUrls();
        this.adFinishedUrls = audioAd.getFinishUrls();
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

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
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

    public boolean isOfflineTrack() {
        return isOfflineTrack;
    }

    public Urn getCreatorUrn() {
        return creatorUrn;
    }

    public String getUUID() {
        return uuid;
    }

    public String getMonetizationModel() {
        return monetizationModel;
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

    public boolean isMarketablePlay() {
        return marketablePlay;
    }

    public boolean isThirdPartyAd() {
        return AdUtils.isThirdPartyAudioAd(trackUrn);
    }
}
