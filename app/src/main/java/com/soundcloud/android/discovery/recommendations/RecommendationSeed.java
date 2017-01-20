package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class RecommendationSeed {
    abstract long seedTrackLocalId();

    abstract Urn seedTrackUrn();

    abstract String seedTrackTitle();

    abstract RecommendationReason reason();

    abstract int queryPosition();

    abstract Urn queryUrn();

    public static RecommendationSeed create(long seedTrackLocalId,
                                            Urn seedTrackUrn,
                                            String seedTrackTitle,
                                            RecommendationReason reason,
                                            int queryPosition,
                                            Urn queryUrn) {
        return new AutoValue_RecommendationSeed(seedTrackLocalId, seedTrackUrn, seedTrackTitle, reason, queryPosition, queryUrn);
    }
}
