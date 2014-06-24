package com.soundcloud.android.model;

import com.soundcloud.propeller.Property;

import java.util.Date;

public final class ActivityProperty {
    public static final int TYPE_FOLLOWER = 0;
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_REPOST = 2;
    public static final int TYPE_LIKE = 3;

    public static final Property<Integer> TYPE = Property.of(Integer.class);
    public static final Property<String> SOUND_TITLE = PlayableProperty.TITLE;
    public static final Property<String> USER_NAME = PlayableProperty.CREATOR_NAME;
    public static final Property<UserUrn> USER_URN = Property.of(UserUrn.class);
    public static final Property<Date> DATE = Property.of(Date.class);
}
