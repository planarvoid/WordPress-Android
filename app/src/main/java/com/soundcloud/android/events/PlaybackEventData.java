package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.tracking.eventlogger.Action;

public class PlaybackEventData {

    private Track mTrack;
    private Action mAction;
    private long mUserId;
    private String mEventLoggerParams;
    private long mTimeStamp;

    public PlaybackEventData(Track track, Action action, long userId, String eventLoggerParams) {
        mTrack = track;
        mAction = action;
        mUserId = userId;
        mEventLoggerParams = eventLoggerParams;
        mTimeStamp = System.currentTimeMillis();
    }

    public Track getTrack() {
        return mTrack;
    }

    public Action getAction() {
        return mAction;
    }

    public long getUserId() {
        return mUserId;
    }

    public String getEventLoggerParams() {
        return mEventLoggerParams;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackEventData.class)
                .add("Track_ID", mTrack.getId())
                .add("Action", mAction)
                .add("UserID", mUserId)
                .add("EventLoggerParams", mEventLoggerParams)
                .add("TimeStamp", mTimeStamp).toString();
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }
}
