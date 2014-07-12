package com.soundcloud.android.users;

import com.soundcloud.propeller.Property;

public final class UserProperty {

    public static final Property<UserUrn> URN = Property.of(UserUrn.class);
    public static final Property<String> USERNAME = Property.of(String.class);
    public static final Property<String> COUNTRY = Property.of(String.class);
    public static final Property<Integer> FOLLOWERS_COUNT = Property.of(Integer.class);
    public static final Property<Boolean> IS_FOLLOWED_BY_ME = Property.of(Boolean.class);

}
