package com.soundcloud.android.users;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.propeller.Property;

public final class UserProperty extends EntityProperty{

    public static final Property<String> USERNAME = Property.of(UserProperty.class, String.class);
    public static final Property<String> COUNTRY = Property.of(UserProperty.class, String.class);
    public static final Property<Integer> FOLLOWERS_COUNT = Property.of(UserProperty.class, Integer.class);
    public static final Property<Boolean> IS_FOLLOWED_BY_ME = Property.of(UserProperty.class, Boolean.class);

}
