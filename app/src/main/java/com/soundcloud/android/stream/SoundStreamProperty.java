package com.soundcloud.android.stream;

import com.soundcloud.java.collections.Property;

import java.util.Date;

public class SoundStreamProperty {
    public static final Property<Date> CREATED_AT = Property.of(SoundStreamProperty.class, Date.class);
}
