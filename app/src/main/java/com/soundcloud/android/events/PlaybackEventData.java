package com.soundcloud.android.events;

import static com.soundcloud.android.tracking.eventlogger.EventLoggerParams.Action;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackEventData {

    private Track mTrack;
    private String mAction;
    private long mUserId;
    private String mTrackSourceParams;
    private long mTimeStamp;
    private long mListenTime;

    public static PlaybackEventData forPlay(Track track, long userId, String trackSourceParams) {
        return new PlaybackEventData(track, Action.PLAY, userId, trackSourceParams);
    }

    public static PlaybackEventData forStop(Track track, long userId, String trackSourceParams, PlaybackEventData lastPlayEvent) {
        final PlaybackEventData playbackEventData = new PlaybackEventData(track, Action.STOP, userId, trackSourceParams);
        playbackEventData.setListenTime(playbackEventData.mTimeStamp - lastPlayEvent.getTimeStamp());
        return playbackEventData;
    }

    private PlaybackEventData(Track track, String action, long userId, String trackSourceParams) {
        mTrack = track;
        mAction = action;
        mUserId = userId;
        mTrackSourceParams = trackSourceParams;
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

    public String getEventLoggerParams() {
        return mTrackSourceParams;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEventData.class)
                .add("Track_ID", mTrack.getId())
                .add("Action", mAction)
                .add("UserID", mUserId)
                .add("EventLoggerParams", mTrackSourceParams)
                .add("TimeStamp", mTimeStamp).toString();
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public long getListenTime() {
        return mListenTime;
    }
}
