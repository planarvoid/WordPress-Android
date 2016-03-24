package com.soundcloud.android.model;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

public class EntityProperty {
    public static final Property<Urn> URN = Property.of(EntityProperty.class, Urn.class);
    public static final Property<Optional<String>> IMAGE_URL_TEMPLATE =
            Property.ofOptional(EntityProperty.class, String.class);
}
