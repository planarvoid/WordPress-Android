package com.soundcloud.android.stations;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.Property;

// TODO : new playable ?
public class StationProperty extends PlayableProperty {
    public static final Property<String> TYPE = Property.of(StationProperty.class, String.class);
    public static final Property<Long> SEED_TRACK_ID = Property.of(StationProperty.class, Long.class);
    public static final Property<Integer> LAST_PLAYED_TRACK_POSITION = Property.of(StationProperty.class, Integer.class);
}
