package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackSessionEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    private static final String AD_ATTR_URN = "ad_urn";
    private static final String AD_ATTR_MONETIZED_URN = "monetized_urn";
    private static final String AD_ATTR_PROTOCOL = "protocol";

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    private final int kind, duration;
    private final TrackUrn trackUrn;
    private final UserUrn userUrn;
    @Nullable private final String trackPolicy;

    private final TrackSourceInfo trackSourceInfo;
    private final long timeStamp, progress;
    private long listenTime;
    private int stopReason;

    // extra meta data specific to audio ad events
    private Map<String, String> audioAdAttributes = Collections.emptyMap();

    public static PlaybackSessionEvent forAdPlay(AudioAd audioAd, TrackUrn monetizableTrackUrn, UserUrn userUrn,
                                                 PlaybackProtocol protocol, TrackSourceInfo trackSourceInfo,
                                                 long progress, long timestamp) {
        return new PlaybackSessionEvent(audioAd, monetizableTrackUrn, userUrn, protocol, trackSourceInfo,
                progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackData, userUrn, trackSourceInfo, progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               TrackSourceInfo trackSourceInfo, long progress) {
        return forPlay(trackData, userUrn, trackSourceInfo, progress, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress, long timestamp) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackData, userUrn, trackSourceInfo, progress, timestamp);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timeStamp - lastPlayEvent.getTimeStamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress) {
        return forStop(trackData, userUrn, trackSourceInfo, lastPlayEvent, stopReason, progress, System.currentTimeMillis());
    }

    private static PropertySet audioAdPropertySet(AudioAd audioAd) {
        return PropertySet.from(
                TrackProperty.URN.bind(audioAd.getApiTrack().getUrn()),
                PlayableProperty.DURATION.bind(audioAd.getApiTrack().getDuration()));
    }

    // Use this constructor for an ordinary audio playback event
    private PlaybackSessionEvent(int eventKind, PropertySet adTrackData, UserUrn userUrn,
                                 TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this.trackUrn = adTrackData.get(TrackProperty.URN);
        this.trackPolicy = adTrackData.getOrElseNull(TrackProperty.POLICY);
        this.duration = adTrackData.get(PlayableProperty.DURATION);
        this.kind = eventKind;
        this.userUrn = userUrn;
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.timeStamp = timestamp;
    }

    // Use this constructor for an audio ad playback event
    private PlaybackSessionEvent(AudioAd audioAd, TrackUrn monetizableTrackUrn, UserUrn userUrn, PlaybackProtocol protocol,
                                 TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this(EVENT_KIND_PLAY, audioAdPropertySet(audioAd), userUrn, trackSourceInfo, progress, timestamp);
        this.audioAdAttributes = new HashMap<String, String>(3);
        this.audioAdAttributes.put(AD_ATTR_URN, audioAd.getUrn());
        this.audioAdAttributes.put(AD_ATTR_MONETIZED_URN, monetizableTrackUrn.toString());
        this.audioAdAttributes.put(AD_ATTR_PROTOCOL, protocol.getValue());
    }

    public int getKind() {
        return kind;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    @Nullable
    public String getTrackPolicy() {
        return trackPolicy;
    }

    public boolean isPlayEvent() {
        return kind == EVENT_KIND_PLAY;
    }

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public UserUrn getUserUrn() {
        return userUrn;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
    }

    public boolean isPlayingOwnPlaylist() {
        return trackSourceInfo.getPlaylistOwnerId() == userUrn.numericId;
    }

    public int getDuration() {
        return duration;
    }

    public long getProgress() {
        return progress;
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

    public String getAudioAdUrn() {
        return audioAdAttributes.get(AD_ATTR_URN);
    }

    public String getAudioAdMonetizedUrn() {
        return audioAdAttributes.get(AD_ATTR_MONETIZED_URN);
    }

    public String getAudioAdProtocol() {
        return audioAdAttributes.get(AD_ATTR_PROTOCOL);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackSessionEvent.class)
                .add("TrackUrn", trackUrn.toString())
                .add("Duration", duration)
                .add("Event", kind)
                .add("UserUrn", userUrn)
                .add("TrackSourceInfo", trackSourceInfo)
                .add("TimeStamp", timeStamp).toString();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getListenTime() {
        return listenTime;
    }

    public boolean isAd() {
        return !audioAdAttributes.isEmpty();
    }

    public boolean isAtStart() {
        return getProgress() == 0L;
    }
}
