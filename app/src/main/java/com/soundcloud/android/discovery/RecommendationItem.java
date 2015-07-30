package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

public class RecommendationItem {

    private final PropertySet source;

    public RecommendationItem(PropertySet source) {
        this.source = source;
    }

    public static Func1<List<PropertySet>, List<RecommendationItem>> fromPropertySets() {
        return new Func1<List<PropertySet>, List<RecommendationItem>>() {
            @Override
            public List<RecommendationItem> call(List<PropertySet> bindings) {
                List<RecommendationItem> recommendationItems = new ArrayList<>(bindings.size());
                for (PropertySet source : bindings) {
                    recommendationItems.add(from(source));
                }
                return recommendationItems;
            }
        };
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

    private static RecommendationItem from(PropertySet source) {
        return new RecommendationItem(source);
    }
}
