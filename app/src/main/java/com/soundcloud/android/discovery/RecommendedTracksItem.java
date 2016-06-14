package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

@AutoValue
abstract class RecommendedTracksItem extends DiscoveryItem {

    public RecommendedTracksItem() {
        super(Kind.RecommendedTracksItem);
    }

    static RecommendedTracksItem create(PropertySet source, List<Recommendation> recommendations) {
        return new AutoValue_RecommendedTracksItem(source, recommendations);
    }


    String getSeedTrackTitle() {
        return getSource().get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return getSource().get(RecommendationProperty.SEED_TRACK_URN);
    }

    int getSeedTrackQueryPosition() {
        return getSource().get(RecommendationProperty.QUERY_POSITION);
    }

    Urn getQueryUrn() {
        return getSource().get(RecommendationProperty.QUERY_URN);
    }

    abstract PropertySet getSource();

    abstract List<Recommendation> getRecommendations();

    RecommendationReason getRecommendationReason() {
        return getSource().get(RecommendationProperty.REASON);
    }

    long getSeedTrackLocalId() {
        return getSource().get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }
}
