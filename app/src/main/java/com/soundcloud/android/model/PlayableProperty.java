package com.soundcloud.android.model;

import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.Property;

import java.util.Date;

public final class PlayableProperty {
    public static final Property<Urn> URN = Property.of(Urn.class);
    public static final Property<String> TITLE = Property.of(String.class);
    public static final Property<Integer> DURATION = Property.of(Integer.class);
    public static final Property<UserUrn> CREATOR_URN = Property.of(UserUrn.class);
    public static final Property<String> CREATOR_NAME = Property.of(String.class);
    public static final Property<String> REPOSTER = Property.of(String.class);
    public static final Property<Date> CREATED_AT = Property.of(Date.class);
    public static final Property<Integer> LIKES_COUNT = Property.of(Integer.class);
    public static final Property<Integer> REPOSTS_COUNT = Property.of(Integer.class);
    public static final Property<Boolean> IS_LIKED = Property.of(Boolean.class);
    public static final Property<Boolean> IS_REPOSTED = Property.of(Boolean.class);
    public static final Property<Boolean> IS_PRIVATE = Property.of(Boolean.class);
}
