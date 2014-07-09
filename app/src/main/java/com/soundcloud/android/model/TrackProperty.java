package com.soundcloud.android.model;

import com.soundcloud.propeller.Property;

public final class TrackProperty {
    public static final Property<TrackUrn> URN = Property.of(TrackUrn.class);
    public static final Property<Integer> PLAY_COUNT = Property.of(Integer.class);
    public static final Property<String> WAVEFORM_URL = Property.of(String.class);
    public static final Property<Boolean> MONETIZABLE = Property.of(Boolean.class);
}
