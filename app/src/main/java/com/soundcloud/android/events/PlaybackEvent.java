package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import org.jetbrains.annotations.NotNull;

public final class PlaybackEvent {

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
    @NotNull
    private final Track track;

    private final long userId;
    private final TrackSourceInfo trackSourceInfo;
    private final long timeStamp;

    private int stopReason;
    private long listenTime;

    public static PlaybackEvent forPlay(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, long timestamp) {
        return new PlaybackEvent(EVENT_KIND_PLAY, track, userId, trackSourceInfo, timestamp);
    }

    public static PlaybackEvent forPlay(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo) {
        return forPlay(track, userId, trackSourceInfo, System.currentTimeMillis());
    }

    public static PlaybackEvent forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo,
                                            PlaybackEvent lastPlayEvent, int stopReason, long timestamp) {
        final PlaybackEvent playbackEvent =
                new PlaybackEvent(EVENT_KIND_STOP, track, userId, trackSourceInfo, timestamp);
        playbackEvent.setListenTime(playbackEvent.timeStamp - lastPlayEvent.getTimeStamp());
        playbackEvent.setStopReason(stopReason);
        return playbackEvent;
    }

    public static PlaybackEvent forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo,
                                           PlaybackEvent lastPlayEvent,  int stopReason) {
        return forStop(track, userId, trackSourceInfo, lastPlayEvent, stopReason, System.currentTimeMillis());
    }

    private PlaybackEvent(int eventKind, @NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, long timestamp) {
        this.track = track;
        this.kind = eventKind;
        this.userId = userId;
        this.trackSourceInfo = trackSourceInfo;
        this.timeStamp = timestamp;
    }

    public int getKind() {
        return kind;
    }

    public Track getTrack() {
        return track;
    }

    public boolean isPlayEvent() {
        return kind == EVENT_KIND_PLAY;
    }

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public long getUserId() {
        return userId;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
    }

    public boolean isPlayingOwnPlaylist(){
        return trackSourceInfo.getPlaylistOwnerId() == userId;
    }

    public int getStopReason() {
        return stopReason;
    }

    private void setListenTime(long listenTime) {
        this.listenTime = listenTime;
    }

    private void setStopReason(int stopReason) {
        this.stopReason = stopReason;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEvent.class)
                .add("Track_ID", track.getId())
                .add("Event", kind)
                .add("UserID", userId)
                .add("TrackSourceInfo", trackSourceInfo)
                .add("TimeStamp", timeStamp)
                .add("StopReason", stopReason).toString();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getListenTime() {
        return listenTime;
    }
}
