package com.soundcloud.android.model;

import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.Property;

import java.util.Date;

public final class PlayableProperty {
    public static final Property<Urn> URN = Property.of(PlayableProperty.class, Urn.class);
    public static final Property<String> TITLE = Property.of(PlayableProperty.class, String.class);
    public static final Property<Integer> DURATION = Property.of(PlayableProperty.class, Integer.class);
    public static final Property<UserUrn> CREATOR_URN = Property.of(PlayableProperty.class, UserUrn.class);
    public static final Property<String> CREATOR_NAME = Property.of(PlayableProperty.class, String.class);
    public static final Property<String> REPOSTER = Property.of(PlayableProperty.class, String.class);
    public static final Property<Date> CREATED_AT = Property.of(PlayableProperty.class, Date.class);
    public static final Property<Integer> LIKES_COUNT = Property.of(PlayableProperty.class, Integer.class);
    public static final Property<Integer> REPOSTS_COUNT = Property.of(PlayableProperty.class, Integer.class);
    public static final Property<Boolean> IS_LIKED = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<Boolean> IS_REPOSTED = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<Boolean> IS_PRIVATE = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<String> PERMALINK_URL = Property.of(PlayableProperty.class, String.class);
}
