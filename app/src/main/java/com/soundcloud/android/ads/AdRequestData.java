package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.UUID;

@AutoValue
public abstract class AdRequestData {
    private static AdRequestData create(Optional<Urn> monetizableTrackUrn, Optional<String> kruxSegments)  {
        return new AutoValue_AdRequestData(UUID.randomUUID().toString(), monetizableTrackUrn, kruxSegments);
    }

    public static AdRequestData forPlayerAd(Urn monetizableTrackUrn, Optional<String> kruxSegments) {
        return create(Optional.of(monetizableTrackUrn), kruxSegments);
    }

    public static AdRequestData forPageAds(Optional<String> kruxSegments) {
        return create(Optional.absent(), kruxSegments);
    }

    public abstract String getRequestId();

    public abstract Optional<Urn> getMonetizableTrackUrn();

    public abstract Optional<String> getKruxSegments();
}
