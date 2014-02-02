package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueues;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventSource {

    private final EventBus2 mEventBus;
    private PlaybackEvent mLastPlayEventData;

    @Inject
    public PlaybackEventSource(EventBus2 eventBus) {
        mEventBus = eventBus;
    }

    public void publishPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            mLastPlayEventData = PlaybackEvent.forPlay(track, userId, trackSourceInfo);
            mEventBus.publish(EventQueues.PLAYBACK, mLastPlayEventData);
        }
    }

    public void publishStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId, int stopReason) {
        if (mLastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackEvent eventData = PlaybackEvent.forStop(track, userId, trackSourceInfo, mLastPlayEventData, stopReason);
            mEventBus.publish(EventQueues.PLAYBACK, eventData);
            mLastPlayEventData = null;
        }
    }
}
