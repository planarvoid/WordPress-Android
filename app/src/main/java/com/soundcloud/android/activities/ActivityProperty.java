package com.soundcloud.android.activities;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

import java.util.Date;

public final class ActivityProperty {
    public static final Property<ActivityKind> KIND = Property.of(ActivityProperty.class, ActivityKind.class);
    public static final Property<String> PLAYABLE_TITLE = Property.of(ActivityProperty.class, String.class);
    public static final Property<String> USER_NAME = Property.of(ActivityProperty.class, String.class);
    public static final Property<Urn> USER_URN = Property.of(ActivityProperty.class, Urn.class);
    public static final Property<Date> DATE = Property.of(ActivityProperty.class, Date.class);
}
