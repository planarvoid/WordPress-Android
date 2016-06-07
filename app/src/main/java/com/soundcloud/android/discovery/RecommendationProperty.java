package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

class RecommendationProperty {
    static final Property<Long> SEED_TRACK_LOCAL_ID = Property.of(RecommendationProperty.class, Long.class);
    static final Property<Urn> SEED_TRACK_URN = Property.of(RecommendationProperty.class, Urn.class);
    static final Property<String> SEED_TRACK_TITLE = Property.of(RecommendationProperty.class, String.class);
    static final Property<RecommendationReason> REASON = Property.of(RecommendationProperty.class, RecommendationReason.class);
    static final Property<Integer> RECOMMENDED_TRACKS_COUNT = Property.of(RecommendationProperty.class, Integer.class);
    static final Property<Integer> QUERY_POSITION = Property.of(RecommendationProperty.class, Integer.class);
    static final Property<Urn> QUERY_URN = Property.of(RecommendationProperty.class, Urn.class);
}
