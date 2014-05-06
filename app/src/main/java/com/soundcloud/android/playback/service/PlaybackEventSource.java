package com.soundcloud.android.playback.service;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackEventSource {

    private final EventBus eventBus;
    private PlaybackEvent lastPlayEventData;

    @Inject
    public PlaybackEventSource(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publishPlayEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId) {
        if (track != null && trackSourceInfo != null) {
            lastPlayEventData = PlaybackEvent.forPlay(track, userId, trackSourceInfo);
            eventBus.publish(EventQueue.PLAYBACK, lastPlayEventData);
        }
    }

    public void publishStopEvent(@Nullable Track track, @Nullable TrackSourceInfo trackSourceInfo, long userId, int stopReason) {
        if (lastPlayEventData != null && track != null && trackSourceInfo != null) {
            final PlaybackEvent eventData = PlaybackEvent.forStop(track, userId, trackSourceInfo, lastPlayEventData, stopReason);
            eventBus.publish(EventQueue.PLAYBACK, eventData);
            lastPlayEventData = null;
        }
    }
}
