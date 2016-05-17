package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

class RecommendationBucket extends DiscoveryItem {

    private final PropertySet source;
    private final List<Recommendation> recommendations;

    RecommendationBucket(PropertySet source, List<Recommendation> recommendations) {
        super(Kind.TrackRecommendationItem);
        this.source = source;
        this.recommendations = recommendations;
    }

    String getSeedTrackTitle() {
        return source.get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return source.get(RecommendationProperty.SEED_TRACK_URN);
    }

    List<Recommendation> getRecommendations() {
        return recommendations;
    }

    RecommendationReason getRecommendationReason() {
        return source.get(RecommendationProperty.REASON);
    }

    long getSeedTrackLocalId() {
        return source.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }
}
