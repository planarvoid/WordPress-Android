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
        return source.get(SeedSoundProperty.TITLE);
    }

    public Urn getSeedTrackUrn() {
        return source.get(SeedSoundProperty.URN);
    }

    public String getRecommendationTitle() {
        return source.get(RecommendationProperty.TITLE);
    }

    public String getRecommendationUserName() {
        return source.get(RecommendationProperty.USERNAME);
    }

    public int getRecommendationCount() {
        return source.get(SeedSoundProperty.RECOMMENDATION_COUNT);
    }

    public Urn getRecommendationUrn() {
        return source.get(RecommendationProperty.URN);
    }

    public RecommendationReason getRecommendationReason() {
        return source.get(SeedSoundProperty.REASON);
    }

    public long getSeedTrackLocalId() {
        return source.get(SeedSoundProperty.LOCAL_ID);
    }
}
