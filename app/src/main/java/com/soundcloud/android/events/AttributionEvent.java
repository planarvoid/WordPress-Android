package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AttributionEvent extends NewTrackingEvent {
    public static AttributionEvent create(String network, String campaign, String adGroup, String creative) {
        return new AutoValue_AttributionEvent(defaultId(),
                defaultTimestamp(),
                Optional.absent(),
                Optional.fromNullable(network),
                Optional.fromNullable(campaign),
                Optional.fromNullable(adGroup),
                Optional.fromNullable(creative));
    }

    public abstract Optional<String> network();

    public abstract Optional<String> campaign();

    public abstract Optional<String> adGroup();

    public abstract Optional<String> creative();

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AttributionEvent(id(),
                timestamp(),
                Optional.of(referringEvent),
                network(),
                campaign(),
                adGroup(),
                creative());
    }
}
