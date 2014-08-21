package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.util.SparseArray;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackSessionEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    private static final int EXTRA_AD_URN = 0;
    private static final int EXTRA_MONETIZED_URN = 1;
    private static final int EXTRA_TRACK_POLICY = 2;

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    private final int kind, duration;
    private final TrackUrn trackUrn;
    private final UserUrn userUrn;
    private final String protocol;

    private final TrackSourceInfo trackSourceInfo;
    private final long timeStamp, progress;
    private long listenTime;
    private int stopReason;

    // extra meta data that might not always be present goes here
    private SparseArray<String> extraAttributes = new SparseArray<String>();

    public static PlaybackSessionEvent forAdPlay(PropertySet audioAd, PropertySet audioAdTrack, UserUrn userUrn,
                                                 String protocol, TrackSourceInfo trackSourceInfo, long progress) {
        return forAdPlay(audioAd, audioAdTrack, userUrn, protocol, trackSourceInfo, progress, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forAdPlay(PropertySet audioAd, PropertySet audioAdTrack, UserUrn userUrn,
                                                 String protocol, TrackSourceInfo trackSourceInfo,
                                                 long progress, long timestamp) {
        return new PlaybackSessionEvent(audioAd, audioAdTrack, userUrn, protocol, trackSourceInfo, progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress) {
        return forPlay(trackData, userUrn, protocol, trackSourceInfo, progress, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress, long timestamp) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timeStamp - lastPlayEvent.getTimeStamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress) {
        return forStop(trackData, userUrn, protocol, trackSourceInfo, lastPlayEvent, stopReason, progress, System.currentTimeMillis());
    }

    // Use this constructor for an ordinary audio playback event
    private PlaybackSessionEvent(int eventKind, PropertySet track, UserUrn userUrn,
                                 String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this.trackUrn = track.get(TrackProperty.URN);
        this.duration = track.get(PlayableProperty.DURATION);
        this.kind = eventKind;
        this.userUrn = userUrn;
        this.protocol = protocol;
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.timeStamp = timestamp;
        if (track.contains(TrackProperty.POLICY)) {
            this.extraAttributes.put(EXTRA_TRACK_POLICY, track.get(TrackProperty.POLICY));
        }
    }

    // Use this constructor for an audio ad playback event
    private PlaybackSessionEvent(PropertySet audioAd, PropertySet audioAdTrack, UserUrn userUrn, String protocol,
                                 TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this(EVENT_KIND_PLAY, audioAdTrack, userUrn, protocol, trackSourceInfo, progress, timestamp);
        this.extraAttributes.put(EXTRA_AD_URN, audioAd.get(AdProperty.AD_URN));
        this.extraAttributes.put(EXTRA_MONETIZED_URN, audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
    }

    public int getKind() {
        return kind;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    @Nullable
    public String getTrackPolicy() {
        return extraAttributes.get(EXTRA_TRACK_POLICY);
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

    public String getProtocol() {
        return protocol;
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
        return extraAttributes.get(EXTRA_AD_URN);
    }

    public String getAudioAdMonetizedUrn() {
        return extraAttributes.get(EXTRA_MONETIZED_URN);
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
        return extraAttributes.indexOfKey(EXTRA_AD_URN) != -1;
    }

    public boolean isFirstPlay() {
        return isPlayEvent() && progress == 0L;
    }
}
