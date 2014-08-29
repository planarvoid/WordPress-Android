package com.soundcloud.android.activities;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.Property;

import java.util.Date;

public final class ActivityProperty {
    public static final int TYPE_FOLLOWER = 0;
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_REPOST = 2;
    public static final int TYPE_LIKE = 3;

    public static final Property<Integer> TYPE = Property.of(ActivityProperty.class, Integer.class);
    public static final Property<String> SOUND_TITLE = PlayableProperty.TITLE;
    public static final Property<String> USER_NAME = PlayableProperty.CREATOR_NAME;
    public static final Property<UserUrn> USER_URN = Property.of(ActivityProperty.class, UserUrn.class);
    public static final Property<Date> DATE = Property.of(ActivityProperty.class, Date.class);
}
