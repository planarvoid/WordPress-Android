package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventTracker {

    private PlaybackEvent mLastPlayEventData;

    @Inject
    public PlaybackEventTracker() {
    }

    public void trackPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            mLastPlayEventData = PlaybackEvent.forPlay(track, userId, trackSourceInfo);
            EventBus.PLAYBACK.publish(mLastPlayEventData);
        }
    }

    public void trackStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId, int stopReason) {
        if (mLastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackEvent eventData = PlaybackEvent.forStop(track, userId, trackSourceInfo, mLastPlayEventData, stopReason);
            EventBus.PLAYBACK.publish(eventData);
            mLastPlayEventData = null;
        }
    }
}
