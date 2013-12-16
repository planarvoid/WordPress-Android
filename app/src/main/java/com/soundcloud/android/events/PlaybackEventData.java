package com.soundcloud.android.events;

import static com.soundcloud.android.tracking.eventlogger.EventLoggerParams.Action;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackEventData {

    @NotNull
    private Track mTrack;

    private String mAction;
    private long mUserId;
    private TrackSourceInfo mTrackSourceInfo;
    private long mTimeStamp;

    private long mListenTime;

    public static PlaybackEventData forPlay(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo) {
        return new PlaybackEventData(track, Action.PLAY, userId, trackSourceInfo);
    }

    public static PlaybackEventData forStop(@NotNull Track track, long userId, TrackSourceInfo trackSourceInfo, PlaybackEventData lastPlayEvent) {
        final PlaybackEventData playbackEventData = new PlaybackEventData(track, Action.STOP, userId, trackSourceInfo);
        playbackEventData.setListenTime(playbackEventData.mTimeStamp - lastPlayEvent.getTimeStamp());
        return playbackEventData;
    }

    private PlaybackEventData(@NotNull Track track, String action, long userId, TrackSourceInfo trackSourceInfo) {
        mTrack = track;
        mAction = action;
        mUserId = userId;
        mTrackSourceInfo = trackSourceInfo;
        mTimeStamp = System.currentTimeMillis();
    }

    public void setListenTime(long listenTime) {
        mListenTime = listenTime;
    }

    public Track getTrack() {
        return mTrack;
    }

    public String getAction() {
        return mAction;
    }

    public long getUserId() {
        return mUserId;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return mTrackSourceInfo;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEventData.class)
                .add("Track_ID", mTrack.getId())
                .add("Action", mAction)
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
