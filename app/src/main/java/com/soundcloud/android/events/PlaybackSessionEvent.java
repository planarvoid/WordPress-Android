package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import org.jetbrains.annotations.NotNull;

public final class PlaybackSessionEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    private final int kind;
    private final TrackUrn trackUrn;
    private final UserUrn userUrn;

    private final TrackSourceInfo trackSourceInfo;
    private final long timeStamp, duration;
    private long listenTime;
    private int stopReason;

    public static PlaybackSessionEvent forPlay(@NotNull TrackUrn trackUrn, UserUrn userUrn, TrackSourceInfo trackSourceInfo, long duration, long timestamp) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackUrn, userUrn, trackSourceInfo, duration, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull TrackUrn trackUrn, UserUrn userUrn, TrackSourceInfo trackSourceInfo, long duration) {
        return forPlay(trackUrn, userUrn, trackSourceInfo, duration, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forStop(@NotNull TrackUrn trackUrn, UserUrn userUrn, TrackSourceInfo trackSourceInfo,
                                               PlaybackSessionEvent lastPlayEvent, long duration, int stopReason, long timestamp) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackUrn, userUrn, trackSourceInfo, duration, timestamp);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timeStamp - lastPlayEvent.getTimeStamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    public static PlaybackSessionEvent forStop(@NotNull TrackUrn trackUrn, UserUrn userUrn, TrackSourceInfo trackSourceInfo,
                                               PlaybackSessionEvent lastPlayEvent, long duration, int stopReason) {
        return forStop(trackUrn, userUrn, trackSourceInfo, lastPlayEvent, duration, stopReason, System.currentTimeMillis());
    }

    private PlaybackSessionEvent(int eventKind, @NotNull TrackUrn trackUrn, UserUrn userUrn, TrackSourceInfo trackSourceInfo, long duration, long timestamp) {
        this.trackUrn = trackUrn;
        this.kind = eventKind;
        this.userUrn = userUrn;
        this.trackSourceInfo = trackSourceInfo;
        this.duration = duration;
        this.timeStamp = timestamp;
    }

    public int getKind() {
        return kind;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
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

    public boolean isPlayingOwnPlaylist(){
        return trackSourceInfo.getPlaylistOwnerId() == userUrn.numericId;
    }

    public long getDuration() {
        return duration;
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
}
