package com.soundcloud.android.model;

import java.util.Date;

public final class PlayableProperty {
    public static final Property<Urn> URN = Property.of(Urn.class);
    public static final Property<String> TITLE = Property.of(String.class);
    public static final Property<Integer> DURATION = Property.of(Integer.class);
    public static final Property<String> CREATOR = Property.of(String.class);
    public static final Property<String> REPOSTER = Property.of(String.class);
    public static final Property<Date> REPOSTED_AT = Property.of(Date.class);
    public static final Property<Integer> LIKES_COUNT = Property.of(Integer.class);
}
