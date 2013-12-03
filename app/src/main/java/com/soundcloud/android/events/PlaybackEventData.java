package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.tracking.eventlogger.Action;

public class PlaybackEventData {

    private Track mTrack;
    private Action mAction;
    private long mUserId;
    private String mEventLoggerParams;

    public PlaybackEventData(Track track, Action action, long userId, String eventLoggerParams) {
        this.mTrack = track;
        this.mAction = action;
        this.mUserId = userId;
        this.mEventLoggerParams = eventLoggerParams;
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
                .add("EventLoggerParams", mEventLoggerParams).toString();
    }
}
