package com.soundcloud.android.analytics;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observer;

import javax.inject.Inject;

public class EventTracker {
    private final EventBusV2 eventBus;
    private final FeatureFlags featureFlags;
    private final TrackingStateProvider trackingStateProvider;

    @Inject
    public EventTracker(EventBusV2 eventBus, TrackingStateProvider trackingStateProvider, FeatureFlags featureFlags) {
        this.eventBus = eventBus;
        this.trackingStateProvider = trackingStateProvider;
        this.featureFlags = featureFlags;
    }

    public void trackScreen(ScreenEvent event, Optional<ReferringEvent> referringEvent) {
        publishAndUpdate(event, referringEvent);
    }

    public void trackForegroundEvent(ForegroundEvent event) {
        publishAndUpdate(event, Optional.absent());
    }

    public void trackSearch(SearchEvent searchEvent) {
        attachAndPublish(searchEvent, trackingStateProvider.getLastEvent());
    }

    public void trackUpgradeFunnel(UpgradeFunnelEvent upgradeFunnelEvent) {
        attachAndPublish(upgradeFunnelEvent, trackingStateProvider.getLastEvent());
    }

    public void trackEngagement(UIEvent event) {
        attachAndPublish(event, trackingStateProvider.getLastEvent());
    }

    Observer<UIEvent> trackEngagementSubscriber() {
        return new DefaultObserver<UIEvent>() {
            @Override
            public void onNext(UIEvent uiEvent) {
                trackEngagement(uiEvent);
            }
        };
    }

    public void trackNavigation(UIEvent event) {
        if (featureFlags.isEnabled(Flag.HOLISTIC_TRACKING)) {
            publishAndUpdate(event, trackingStateProvider.getLastEvent());
        }
    }

    public void trackClick(UIEvent event) {
        publishAndUpdate(event, trackingStateProvider.getLastEvent());
    }

    private void attachAndPublish(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        eventBus.publish(EventQueue.TRACKING, attachReferringEvent(event, referringEvent));
    }

    private void publishAndUpdate(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        attachAndPublish(event, referringEvent);

        trackingStateProvider.update(ReferringEvent.create(event.id(), event.getKind()));
    }

    private TrackingEvent attachReferringEvent(TrackingEvent event, Optional<ReferringEvent> referringEvent) {
        return referringEvent.isPresent() ? event.putReferringEvent(referringEvent.get()) : event;
    }
}
