package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

class RecommendationBucket extends DiscoveryItem {

    private final PropertySet source;
    private final List<RecommendationViewModel> recommendations;
    private final boolean isViewAllBucket;

    RecommendationBucket(PropertySet source, List<RecommendationViewModel> recommendations, boolean isViewAllBucket) {
        super(Kind.TrackRecommendationItem);
        this.source = source;
        this.recommendations = recommendations;
        this.isViewAllBucket = isViewAllBucket;
    }

    String getSeedTrackTitle() {
        return source.get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return source.get(RecommendationProperty.SEED_TRACK_URN);
    }

    List<RecommendationViewModel> getRecommendations() {
        return recommendations;
    }

    RecommendationReason getRecommendationReason() {
        return source.get(RecommendationProperty.REASON);
    }

    long getSeedTrackLocalId() {
        return source.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }

    public boolean isViewAllBucket() {
        return isViewAllBucket;
    }
}
