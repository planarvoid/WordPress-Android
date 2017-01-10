package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AttributionEvent extends NewTrackingEvent {
    public static AttributionEvent create(String network, String campaign, String adGroup, String creative) {
        return new AutoValue_AttributionEvent(defaultId(), defaultTimestamp(), Optional.absent(), network, Optional.of(campaign), adGroup, creative);
    }

    public abstract String network();
    public abstract Optional<String> campaign();
    public abstract String adGroup();
    public abstract String creative();

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AttributionEvent(id(), timestamp(), Optional.of(referringEvent), network(), campaign(), adGroup(), creative());
    }
}
