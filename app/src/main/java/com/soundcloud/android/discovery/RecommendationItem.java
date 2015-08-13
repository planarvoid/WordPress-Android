package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

public class RecommendationItem extends DiscoveryItem {

    private final PropertySet source;

    public RecommendationItem(PropertySet source) {
        super(Kind.TrackRecommendationItem);
        this.source = source;
    }

    public String getSeedTrackTitle() {
        return source.get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    public Urn getSeedTrackUrn() {
        return source.get(RecommendationProperty.SEED_TRACK_URN);
    }

    public String getRecommendationTitle() {
        return source.get(RecommendedTrackProperty.TITLE);
    }

    public String getRecommendationUserName() {
        return source.get(RecommendedTrackProperty.USERNAME);
    }

    public int getRecommendationCount() {
        return source.get(RecommendationProperty.RECOMMENDED_TRACKS_COUNT);
    }

    public Urn getRecommendationUrn() {
        return source.get(RecommendedTrackProperty.URN);
    }

    public RecommendationReason getRecommendationReason() {
        return source.get(RecommendationProperty.REASON);
    }

    public long getSeedTrackLocalId() {
        return source.get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }
}
