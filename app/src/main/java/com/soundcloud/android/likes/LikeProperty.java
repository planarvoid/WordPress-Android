package com.soundcloud.android.likes;

import com.soundcloud.java.collections.Property;

import java.util.Date;

public class LikeProperty {
    public static final Property<Date> CREATED_AT = Property.of(LikeProperty.class, Date.class);
}
