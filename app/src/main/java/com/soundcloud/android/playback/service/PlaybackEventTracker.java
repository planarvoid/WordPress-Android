package com.soundcloud.android.playback.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public class PlaybackEventTracker {

    public static PlaybackEventTracker EMPTY = new PlaybackEventTracker(null, null, User.NOT_SET);

    private final String mParams;
    private final Track mTrack;
    private final long mUserId;

    private PlaybackEventData mLastPlayEventData;

    public PlaybackEventTracker(Track currentTrack, String eventLoggerParams) {
        this(currentTrack, eventLoggerParams, SoundCloudApplication.getUserId());
    }

    public PlaybackEventTracker(Track currentTrack, String eventLoggerParams, long userId) {
        mTrack = currentTrack;
        mParams = eventLoggerParams;
        mUserId = userId;
    }

    public void trackPlayEvent() {
        mLastPlayEventData = PlaybackEventData.forPlay(mTrack, mUserId, mParams);
        Event.PLAYBACK.publish(mLastPlayEventData);
    }

    public void trackStopEvent() {
        if (mLastPlayEventData != null){
            final PlaybackEventData eventData = PlaybackEventData.forStop(mTrack, mUserId, mParams, mLastPlayEventData);
            Event.PLAYBACK.publish(eventData);
            mLastPlayEventData = null;
        }

    }
}
