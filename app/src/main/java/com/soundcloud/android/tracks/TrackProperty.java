package com.soundcloud.android.tracks;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

public class TrackProperty extends PlayableProperty {
    public static final Property<Integer> PLAY_COUNT = Property.of(TrackProperty.class, Integer.class);
    public static final Property<String> WAVEFORM_URL = Property.of(TrackProperty.class, String.class);
    public static final Property<String> DESCRIPTION = Property.of(TrackProperty.class, String.class);
    public static final Property<Integer> COMMENTS_COUNT = Property.of(TrackProperty.class, Integer.class);
    public static final Property<String> STREAM_URL = Property.of(TrackProperty.class, String.class);
    public static final Property<Optional<String>> GENRE = Property.ofOptional(TrackProperty.class, String.class);

    public static final Property<Boolean> MONETIZABLE = Property.of(TrackProperty.class, Boolean.class);
    public static final Property<Boolean> SYNCABLE = Property.of(TrackProperty.class, Boolean.class);
    public static final Property<String> POLICY = Property.of(TrackProperty.class, String.class);
    public static final Property<Boolean> SUB_MID_TIER = Property.of(TrackProperty.class, Boolean.class);
    public static final Property<String> MONETIZATION_MODEL = Property.of(TrackProperty.class, String.class);

}
