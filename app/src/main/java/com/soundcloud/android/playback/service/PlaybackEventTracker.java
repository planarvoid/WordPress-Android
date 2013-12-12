package com.soundcloud.android.playback.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public class PlaybackEventTracker {

    public static final PlaybackEventTracker EMPTY = new PlaybackEventTracker(null, null, User.NOT_SET);

    private final TrackSourceInfo mTrackSourceInfo;
    private final Track mTrack;
    private final long mUserId;

    private PlaybackEventData mLastPlayEventData;

    public PlaybackEventTracker(Track currentTrack, TrackSourceInfo trackSourceInfo) {
        this(currentTrack, trackSourceInfo, SoundCloudApplication.getUserId());
    }

    public PlaybackEventTracker(Track currentTrack, TrackSourceInfo trackSourceInfo, long userId) {
        mTrack = currentTrack;
        mTrackSourceInfo = trackSourceInfo;
        mUserId = userId;
    }

    public void trackPlayEvent() {
        mLastPlayEventData = PlaybackEventData.forPlay(mTrack, mUserId, mTrackSourceInfo);
        Event.PLAYBACK.publish(mLastPlayEventData);
    }

    public void trackStopEvent() {
        if (mLastPlayEventData != null){
            final PlaybackEventData eventData = PlaybackEventData.forStop(mTrack, mUserId, mTrackSourceInfo, mLastPlayEventData);
            Event.PLAYBACK.publish(eventData);
            mLastPlayEventData = null;
        }

    }
}
