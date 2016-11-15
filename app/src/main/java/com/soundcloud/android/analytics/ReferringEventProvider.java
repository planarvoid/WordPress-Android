package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.java.optional.Optional;

import android.os.Bundle;

import javax.inject.Inject;

public class ReferringEventProvider {

    private final TrackingStateProvider trackingStateProvider;

    private Optional<ReferringEvent> referringEvent = Optional.absent();

    @Inject
    ReferringEventProvider(TrackingStateProvider trackingStateProvider) {
        this.trackingStateProvider = trackingStateProvider;
    }

    public void setupReferringEvent() {
        referringEvent = trackingStateProvider.getLastEvent();
    }

    public void restoreReferringEvent(Bundle bundle) {
        if (bundle != null) {
            final ReferringEvent restoredReferringEvent = bundle.getParcelable(ReferringEvent.REFERRING_EVENT_KEY);

            if (restoredReferringEvent != null) {
                referringEvent = Optional.of(restoredReferringEvent);
            }
        }
    }

    public void saveReferringEvent(Bundle bundle) {
        if (referringEvent.isPresent()) {
            bundle.putParcelable(ReferringEvent.REFERRING_EVENT_KEY, referringEvent.get());
        }
    }

    public Optional<ReferringEvent> getReferringEvent() {
        return referringEvent;
    }
}
