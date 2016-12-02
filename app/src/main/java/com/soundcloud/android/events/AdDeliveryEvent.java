package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AdDeliveryEvent extends NewTrackingEvent {

    public abstract Urn adUrn();

    public abstract Optional<Urn> monetizableUrn();

    public abstract String adRequestId();

    public abstract boolean inForeground();

    public abstract boolean playerVisible();

    public static AdDeliveryEvent adDelivered(Optional<Urn> monetizableUrn, Urn adUrn, String adRequestId, boolean playerVisible, boolean inForeground) {
        return new AutoValue_AdDeliveryEvent.Builder().id(defaultId())
                                                      .timestamp(defaultTimestamp())
                                                      .referringEvent(Optional.absent())
                                                      .adUrn(adUrn)
                                                      .monetizableUrn(monetizableUrn)
                                                      .adRequestId(adRequestId)
                                                      .inForeground(inForeground)
                                                      .playerVisible(playerVisible)
                                                      .build();
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdDeliveryEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder adUrn(Urn adUrn);

        public abstract Builder monetizableUrn(Optional<Urn> monetizableUrn);

        public abstract Builder adRequestId(String adRequestId);

        public abstract Builder inForeground(boolean inForeground);

        public abstract Builder playerVisible(boolean playerVisible);

        public abstract AdDeliveryEvent build();
    }
}
