package com.soundcloud.android.model;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public class PlayableProperty extends EntityProperty {
    public static final Property<String> TITLE = Property.of(PlayableProperty.class, String.class);
    public static final Property<Urn> CREATOR_URN = Property.of(PlayableProperty.class, Urn.class);
    public static final Property<String> CREATOR_NAME = Property.of(PlayableProperty.class, String.class);
    public static final Property<Date> CREATED_AT = Property.of(PlayableProperty.class, Date.class);
    public static final Property<Integer> LIKES_COUNT = Property.of(PlayableProperty.class, Integer.class);
    public static final Property<Integer> REPOSTS_COUNT = Property.of(PlayableProperty.class, Integer.class);
    public static final Property<Boolean> IS_USER_LIKE = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<Boolean> IS_USER_REPOST = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<Boolean> IS_PRIVATE = Property.of(PlayableProperty.class, Boolean.class);
    public static final Property<String> PERMALINK_URL = Property.of(PlayableProperty.class, String.class);
    public static final Property<Optional<String>> GENRE = Property.ofOptional(PlayableProperty.class, String.class);
}
