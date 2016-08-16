package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrackingStateProvider {
    private Optional<ReferringEvent> lastEvent = Optional.absent();

    @Inject
    public TrackingStateProvider() {}

    public void update(ReferringEvent referringEvent) {
        this.lastEvent = Optional.of(referringEvent);
    }

    public Optional<ReferringEvent> getLastEvent() {
        return lastEvent;
    }
}
