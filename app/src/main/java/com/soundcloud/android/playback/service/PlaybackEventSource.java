package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventSource {

    private final EventBus mEventBus;
    private PlaybackEvent mLastPlayEventData;

    @Inject
    public PlaybackEventSource(EventBus eventBus) {
        mEventBus = eventBus;
    }

    public void publishPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            mLastPlayEventData = PlaybackEvent.forPlay(track, userId, trackSourceInfo);
            mEventBus.publish(EventQueue.PLAYBACK, mLastPlayEventData);
        }
    }

    public void publishStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId, int stopReason) {
        if (mLastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackEvent eventData = PlaybackEvent.forStop(track, userId, trackSourceInfo, mLastPlayEventData, stopReason);
            mEventBus.publish(EventQueue.PLAYBACK, eventData);
            mLastPlayEventData = null;
        }
    }
}
