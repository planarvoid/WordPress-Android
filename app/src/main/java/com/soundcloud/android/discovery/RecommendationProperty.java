package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

public class RecommendationProperty {
    public static final Property<Urn> URN = Property.of(RecommendationProperty.class, Urn.class);
    public static final Property<String> TITLE = Property.of(RecommendationProperty.class, String.class);
    public static final Property<String> USERNAME = Property.of(RecommendationProperty.class, String.class);
    public static final Property<Urn> SEED_SOUND_URN = Property.of(RecommendationProperty.class, Urn.class);
}
