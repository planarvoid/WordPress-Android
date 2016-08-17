package com.soundcloud.android.analytics;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class EventTracker {
    private final EventBus eventBus;
    private final TrackingStateProvider trackingStateProvider;

    @Inject
    public EventTracker(EventBus eventBus, TrackingStateProvider trackingStateProvider) {
        this.eventBus = eventBus;
        this.trackingStateProvider = trackingStateProvider;
    }

    public void trackScreen(ScreenEvent event, Optional<ReferringEvent> referringEvent) {
        publishAndUpdate(event, referringEvent);
    }

    public void trackForegroundEvent(ForegroundEvent event) {
        publishAndUpdate(event, Optional.<ReferringEvent>absent());
    }

    public void trackEngagement(UIEvent event) {
        attachAndPublish(event, trackingStateProvider.getLastEvent());
    }

    private void attachAndPublish(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        eventBus.publish(EventQueue.TRACKING, attachReferringEvent(event, referringEvent));
    }

    private void publishAndUpdate(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        attachAndPublish(event, referringEvent);

        trackingStateProvider.update(ReferringEvent.create(event.getId(), event.getKind()));
    }

    private TrackingEvent attachReferringEvent(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        if (referringEvent.isPresent()) {
            event.putReferringEvent(referringEvent.get());
        }

        return event;
    }
}
