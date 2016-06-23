package com.soundcloud.android.model;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;

import java.util.Comparator;
import java.util.Date;

public class PostProperty {
    public static final Property<Urn> TARGET_URN = EntityProperty.URN;
    public static final Property<Date> CREATED_AT = Property.of(PostProperty.class, Date.class);
    public static final Property<Boolean> IS_REPOST = Property.of(PostProperty.class, Boolean.class);
    public static final Property<String> REPOSTER = Property.of(PostProperty.class, String.class);
    public static final Property<Urn> REPOSTER_URN = Property.of(PostProperty.class, Urn.class);

    public static final Comparator<PropertySet> COMPARATOR = new Comparator<PropertySet>() {
        @Override
        public int compare(PropertySet lhs, PropertySet rhs) {
            int result = lhs.get(TARGET_URN).compareTo(rhs.get(TARGET_URN));
            if (result == 0) {
                result = lhs.get(IS_REPOST).compareTo(rhs.get(IS_REPOST));
            }
            return result == 0 ? lhs.get(CREATED_AT).compareTo(rhs.get(CREATED_AT)) : result;
        }
    };
}

