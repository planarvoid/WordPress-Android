package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackEventData {

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    @NotNull
    private Track mTrack;

    private int mEventKind;
    private long mUserId;
    private TrackSourceInfo mTrackSourceInfo;
    private long mTimeStamp;

    private long mListenTime;

    public static PlaybackEventData forPlay(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, long timestamp) {
        return new PlaybackEventData(EVENT_KIND_PLAY, track, userId, trackSourceInfo, timestamp);
    }

    public static PlaybackEventData forPlay(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo) {
        return forPlay(track, userId, trackSourceInfo, System.currentTimeMillis());
    }

    public static PlaybackEventData forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo,
                                            PlaybackEventData lastPlayEvent, long timestamp) {
        final PlaybackEventData playbackEventData =
                new PlaybackEventData(EVENT_KIND_STOP, track, userId, trackSourceInfo, timestamp);
        playbackEventData.setListenTime(playbackEventData.mTimeStamp - lastPlayEvent.getTimeStamp());
        return playbackEventData;
    }

    public static PlaybackEventData forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo,
                                            PlaybackEventData lastPlayEvent) {
        return forStop(track, userId, trackSourceInfo, lastPlayEvent, System.currentTimeMillis());
    }

    private PlaybackEventData(int eventKind, @NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, long timestamp) {
        mTrack = track;
        mEventKind = eventKind;
        mUserId = userId;
        mTrackSourceInfo = trackSourceInfo;
        mTimeStamp = timestamp;
    }

    public void setListenTime(long listenTime) {
        mListenTime = listenTime;
    }

    public Track getTrack() {
        return mTrack;
    }

    public boolean isPlayEvent() {
        return mEventKind == EVENT_KIND_PLAY;
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

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEventData.class)
                .add("Track_ID", mTrack.getId())
                .add("Event", mEventKind)
                .add("UserID", mUserId)
                .add("TrackSourceInfo", mTrackSourceInfo)
                .add("TimeStamp", mTimeStamp).toString();
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public long getListenTime() {
        return mListenTime;
    }
}
