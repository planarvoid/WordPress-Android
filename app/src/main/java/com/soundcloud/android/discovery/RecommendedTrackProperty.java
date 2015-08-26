package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

class RecommendedTrackProperty {
    static final Property<Urn> URN = Property.of(RecommendedTrackProperty.class, Urn.class);
    static final Property<String> TITLE = Property.of(RecommendedTrackProperty.class, String.class);
    static final Property<String> USERNAME = Property.of(RecommendedTrackProperty.class, String.class);
    static final Property<Urn> SEED_SOUND_URN = Property.of(RecommendedTrackProperty.class, Urn.class);
}
