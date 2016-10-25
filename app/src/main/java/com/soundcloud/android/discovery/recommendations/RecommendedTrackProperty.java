package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

class RecommendedTrackProperty {
    static final Property<Urn> URN = Property.of(RecommendedTrackProperty.class, Urn.class);
    static final Property<Optional<String>> IMAGE_URL_TEMPLATE = Property.ofOptional(RecommendedTrackProperty.class,
                                                                                     String.class);
    static final Property<String> TITLE = Property.of(RecommendedTrackProperty.class, String.class);
    static final Property<String> USERNAME = Property.of(RecommendedTrackProperty.class, String.class);
    static final Property<Urn> SEED_SOUND_URN = Property.of(RecommendedTrackProperty.class, Urn.class);
}
