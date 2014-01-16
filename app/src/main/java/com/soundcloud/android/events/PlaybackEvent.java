package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import org.jetbrains.annotations.NotNull;

public final class PlaybackEvent implements Event {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    private final int mKind;
    @NotNull
    private final Track mTrack;

    private long mUserId;
    private TrackSourceInfo mTrackSourceInfo;
    private long mTimeStamp;
    private int mStopReason;
    private long mListenTime;

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
        playbackEvent.setListenTime(playbackEvent.mTimeStamp - lastPlayEvent.getTimeStamp());
        playbackEvent.setStopReason(stopReason);
        return playbackEvent;
    }

    public static PlaybackEvent forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo,
                                           PlaybackEvent lastPlayEvent,  int stopReason) {
        return forStop(track, userId, trackSourceInfo, lastPlayEvent, stopReason, System.currentTimeMillis());
    }

    private PlaybackEvent(int eventKind, @NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, long timestamp) {
        mTrack = track;
        mKind = eventKind;
        mUserId = userId;
        mTrackSourceInfo = trackSourceInfo;
        mTimeStamp = timestamp;
    }

    @Override
    public int getKind() {
        return mKind;
    }

    public Track getTrack() {
        return mTrack;
    }

    public boolean isPlayEvent() {
        return mKind == EVENT_KIND_PLAY;
    }

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public long getUserId() {
        return mUserId;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return mTrackSourceInfo;
    }

    public boolean isPlayingOwnPlaylist(){
        return mTrackSourceInfo.getPlaylistOwnerId() == mUserId;
    }

    public int getStopReason() {
        return mStopReason;
    }

    private void setListenTime(long listenTime) {
        mListenTime = listenTime;
    }

    private void setStopReason(int stopReason) {
        mStopReason = stopReason;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEvent.class)
                .add("Track_ID", mTrack.getId())
                .add("Event", mKind)
                .add("UserID", mUserId)
                .add("TrackSourceInfo", mTrackSourceInfo)
                .add("TimeStamp", mTimeStamp)
                .add("StopReason", mStopReason).toString();
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public long getListenTime() {
        return mListenTime;
    }
}
