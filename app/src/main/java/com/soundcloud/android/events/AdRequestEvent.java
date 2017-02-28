package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AdRequestEvent extends TrackingEvent {

    public static final String EVENT_NAME = "ad_request";

    public abstract boolean adsRequestSuccess();

    public abstract Optional<AdsReceived> adsReceived();

    public abstract boolean inForeground();

    public abstract boolean playerVisible();

    public abstract Optional<Urn> monetizableTrackUrn();

    public abstract String adsEndpoint();

    private static AdRequestEvent create(boolean success, String uuid, Optional<Urn> monetizableUrn, String endpoint,
                                         boolean playerVisible, boolean inForeground, Optional<AdsReceived> adsReceived) {
        return new AutoValue_AdRequestEvent(uuid, defaultTimestamp(), Optional.absent(), success, adsReceived, inForeground, playerVisible, monetizableUrn, endpoint);
    }

    public static AdRequestEvent adRequestSuccess(String uuid, Optional<Urn> monetizableUrn, String endpoint,
                                                  AdsReceived adsReceived, boolean playerVisible, boolean inForeground) {
        return AdRequestEvent.create(true,
                                     uuid,
                                     monetizableUrn,
                                     endpoint,
                                     playerVisible,
                                     inForeground,
                                     Optional.of(adsReceived));
    }

    public static AdRequestEvent adRequestFailure(String uuid, Optional<Urn> monetizableUrn, String endpoint,
                                                  boolean playerVisible, boolean inForeground) {
        return AdRequestEvent.create(false,
                                     uuid,
                                     monetizableUrn,
                                     endpoint,
                                     playerVisible,
                                     inForeground,
                                     Optional.absent());
    }

    @Override
    public AdRequestEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdRequestEvent(id(), timestamp(), Optional.of(referringEvent), adsRequestSuccess(), adsReceived(), inForeground(), playerVisible(), monetizableTrackUrn(), adsEndpoint());
    }
}
