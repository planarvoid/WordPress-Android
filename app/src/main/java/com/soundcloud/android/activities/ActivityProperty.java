package com.soundcloud.android.activities;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

import java.util.Date;

public final class ActivityProperty {
    public static final int TYPE_FOLLOWER = 0;
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_REPOST = 2;
    public static final int TYPE_LIKE = 3;
    public static final int TYPE_USER_MENTION = 4;

    public static final Property<Integer> TYPE = Property.of(ActivityProperty.class, Integer.class);
    public static final Property<String> SOUND_TITLE = Property.of(ActivityProperty.class, String.class);
    public static final Property<String> USER_NAME = Property.of(ActivityProperty.class, String.class);
    public static final Property<Urn> USER_URN = Property.of(ActivityProperty.class, Urn.class);
    public static final Property<Date> DATE = Property.of(ActivityProperty.class, Date.class);
}
