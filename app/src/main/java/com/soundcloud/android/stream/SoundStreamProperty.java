package com.soundcloud.android.stream;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public class SoundStreamProperty {
    public static final Property<Optional<String>> AVATAR_URL_TEMPLATE = Property.ofOptional(SoundStreamProperty.class, String.class);
    public static final Property<Date> CREATED_AT = Property.of(SoundStreamProperty.class, Date.class);
}
