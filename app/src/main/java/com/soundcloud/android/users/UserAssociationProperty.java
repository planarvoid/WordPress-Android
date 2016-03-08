package com.soundcloud.android.users;

import com.soundcloud.java.collections.Property;

import java.util.Date;

public class UserAssociationProperty {
    public static final Property<Long> POSITION = Property.of(UserAssociationProperty.class, Long.class);
    public static final Property<Date> ADDED_AT = Property.of(UserAssociationProperty.class, Date.class);
    public static final Property<Date> REMOVED_AT = Property.of(UserAssociationProperty.class, Date.class);
}
