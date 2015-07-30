package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

public class SeedSoundProperty {
    public static final Property<Long> LOCAL_ID = Property.of(SeedSoundProperty.class, Long.class);
    public static final Property<Urn> URN = Property.of(SeedSoundProperty.class, Urn.class);
    public static final Property<String> TITLE = Property.of(SeedSoundProperty.class, String.class);
    public static final Property<RecommendationReason> REASON = Property.of(SeedSoundProperty.class, RecommendationReason.class);
    public static final Property<Integer> RECOMMENDATION_COUNT = Property.of(SeedSoundProperty.class, Integer.class);
}
