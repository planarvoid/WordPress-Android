package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

class RecommendationItem extends DiscoveryItem {

    private final PropertySet source;

    RecommendationItem(PropertySet source) {
        super(Kind.TrackRecommendationItem);
        this.source = source;
    }

    String getSeedTrackTitle() {
        return source.get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return source.get(RecommendationProperty.SEED_TRACK_URN);
    }

    String getRecommendationTitle() {
        return source.get(RecommendedTrackProperty.TITLE);
    }

    String getRecommendationUserName() {
        return source.get(RecommendedTrackProperty.USERNAME);
    }

    int getRecommendationCount() {
        return source.get(RecommendationProperty.RECOMMENDED_TRACKS_COUNT);
    }

    Urn getRecommendationUrn() {
        return source.get(RecommendedTrackProperty.URN);
    }

    RecommendationReason getRecommendationReason() {
        return source.get(RecommendationProperty.REASON);
    }

    long getSeedTrackLocalId() {
        return source.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }
}
