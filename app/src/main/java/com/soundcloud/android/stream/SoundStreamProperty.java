package com.soundcloud.android.stream;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

public class SoundStreamProperty {
    public static final Property<Optional<String>> AVATAR_URL_TEMPLATE = Property.ofOptional(SoundStreamProperty.class,
                                                                                             String.class);
}
