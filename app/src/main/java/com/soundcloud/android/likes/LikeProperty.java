package com.soundcloud.android.likes;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

import java.util.Date;

public class LikeProperty {
    public static final Property<Urn> TARGET_URN = PlayableProperty.URN;
    public static final Property<Date> CREATED_AT = Property.of(LikeProperty.class, Date.class);
    public static final Property<Date> ADDED_AT = Property.of(LikeProperty.class, Date.class);
    public static final Property<Date> REMOVED_AT = Property.of(LikeProperty.class, Date.class);
}
