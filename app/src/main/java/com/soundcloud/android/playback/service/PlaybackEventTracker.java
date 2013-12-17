package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventTracker {

    private PlaybackEventData mLastPlayEventData;

    @Inject
    public PlaybackEventTracker() {
    }

    public void trackPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            mLastPlayEventData = PlaybackEventData.forPlay(track, userId, trackSourceInfo);
            Event.PLAYBACK.publish(mLastPlayEventData);
        }
    }

    public void trackStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (mLastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackEventData eventData = PlaybackEventData.forStop(track, userId, trackSourceInfo, mLastPlayEventData, 0);
            Event.PLAYBACK.publish(eventData);
            mLastPlayEventData = null;
        }
    }
}
