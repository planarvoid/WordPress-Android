package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaybackSessionEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

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

    private PlaybackSessionEvent(int eventKind, @NotNull PropertySet trackData, @NotNull UserUrn userUrn,
                                 TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this.trackUrn = trackData.get(TrackProperty.URN);
        this.trackPolicy = trackData.getOrElseNull(TrackProperty.POLICY);
        this.duration = trackData.get(PlayableProperty.DURATION);
        this.kind = eventKind;
        this.userUrn = userUrn;
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.timeStamp = timestamp;
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
        return false;
    }

    public boolean isAtStart() {
        return getProgress() == 0L;
    }
}
