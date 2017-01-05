package com.soundcloud.android.users;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

public final class UserProperty extends EntityProperty {

    public static final Property<String> USERNAME = Property.of(UserProperty.class, String.class);
    public static final Property<String> COUNTRY = Property.of(UserProperty.class, String.class);
    public static final Property<String> CITY = Property.of(UserProperty.class, String.class);
    public static final Property<Integer> FOLLOWERS_COUNT = Property.of(UserProperty.class, Integer.class);
    public static final Property<Boolean> IS_FOLLOWED_BY_ME = Property.of(UserProperty.class, Boolean.class);
    public static final Property<String> DESCRIPTION = Property.of(UserProperty.class, String.class);
    public static final Property<String> WEBSITE_URL = Property.of(UserProperty.class, String.class);
    public static final Property<String> WEBSITE_NAME = Property.of(UserProperty.class, String.class);
    public static final Property<String> MYSPACE_NAME = Property.of(UserProperty.class, String.class);
    public static final Property<String> DISCOGS_NAME = Property.of(UserProperty.class, String.class);
    public static final Property<Optional<String>> VISUAL_URL = Property.ofOptional(UserProperty.class, String.class);
    public static final Property<Optional<Urn>> ARTIST_STATION = Property.ofOptional(UserProperty.class, Urn.class);
    public static final Property<Long> ID = Property.of(UserProperty.class, Long.class);
}
