package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventSource {

    private final EventBus eventBus;
    private PlaybackSessionEvent lastPlayEventData;

    @Inject
    public PlaybackEventSource(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publishPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            lastPlayEventData = PlaybackSessionEvent.forPlay(track, userId, trackSourceInfo);
            eventBus.publish(EventQueue.PLAYBACK_SESSION, lastPlayEventData);
        }
    }

    public void publishStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId, int stopReason) {
        if (lastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackSessionEvent eventData = PlaybackSessionEvent.forStop(track, userId, trackSourceInfo, lastPlayEventData, stopReason);
            eventBus.publish(EventQueue.PLAYBACK_SESSION, eventData);
            lastPlayEventData = null;
        }
    }
}
