package com.soundcloud.android.stations;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.java.collections.Property;

public class StationProperty extends EntityProperty {
    public static final Property<Integer> POSITION = Property.of(StationProperty.class, Integer.class);
    public static final Property<Long> UPDATED_LOCALLY_AT = Property.of(StationProperty.class, Long.class);
}
