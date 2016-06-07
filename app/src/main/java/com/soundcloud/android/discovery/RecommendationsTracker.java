package com.soundcloud.android.discovery;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RecommendationsEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class RecommendationsTracker {

    private final EventBus eventBus;

    @Inject
    RecommendationsTracker(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void trackPageViewEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(discoveryScreen()));
    }

    void trackRecommendationClick(Screen screen, Urn trackUrn, Urn queryUrn) {
        eventBus.publish(EventQueue.TRACKING, RecommendationsEvent.forTrackClick(screen, trackUrn, queryUrn));
    }

    void trackSeedTrackClick(Screen screen, Urn trackUrn, Urn queryUrn) {
        eventBus.publish(EventQueue.TRACKING, RecommendationsEvent.forSeedTrackClick(screen, trackUrn, queryUrn));
    }

    static Screen discoveryScreen() {
        return Screen.SEARCH_MAIN;
    }

    static Screen recommendationsScreen() {
        return Screen.RECOMMENDATIONS_MAIN;
    }
}
