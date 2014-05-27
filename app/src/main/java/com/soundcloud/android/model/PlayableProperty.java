package com.soundcloud.android.model;

import java.util.Date;

public final class PlayableProperty {
    public static final Property<Urn> URN = Property.of(Urn.class);
    public static final Property<String> TITLE = Property.of(String.class);
    public static final Property<Date> CREATED_AT = Property.of(Date.class);
    public static final Property<String> CREATOR = Property.of(String.class);
    public static final Property<String> REPOSTER = Property.of(String.class);
}
