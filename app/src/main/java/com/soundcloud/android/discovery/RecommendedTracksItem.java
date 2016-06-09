package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

class RecommendedTracksItem extends DiscoveryItem {

    private final PropertySet source;
    private final List<Recommendation> recommendations;

    RecommendedTracksItem(PropertySet source, List<Recommendation> recommendations) {
        super(Kind.RecommendedTracksItem);
        this.source = source;
        this.recommendations = recommendations;
    }

    String getSeedTrackTitle() {
        return source.get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return source.get(RecommendationProperty.SEED_TRACK_URN);
    }

    int getSeedTrackQueryPosition() {
        return source.get(RecommendationProperty.QUERY_POSITION);
    }

    Urn getQueryUrn() {
        return source.get(RecommendationProperty.QUERY_URN);
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
