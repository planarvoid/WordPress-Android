package com.soundcloud.android.tracks;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.Property;

import java.util.Date;

public final class TrackProperty extends PlayableProperty {
    public static final Property<Integer> PLAY_COUNT = Property.of(TrackProperty.class, Integer.class);
    public static final Property<String> WAVEFORM_URL = Property.of(TrackProperty.class, String.class);
    public static final Property<Boolean> MONETIZABLE = Property.of(TrackProperty.class, Boolean.class);
    public static final Property<String> POLICY = Property.of(TrackProperty.class, String.class);
    public static final Property<String> DESCRIPTION = Property.of(TrackProperty.class, String.class);
    public static final Property<Integer> COMMENTS_COUNT = Property.of(TrackProperty.class, Integer.class);
    public static final Property<String> STREAM_URL = Property.of(TrackProperty.class, String.class);
    public static final Property<Date> OFFLINE_DOWNLOADED_AT = Property.of(TrackProperty.class, Date.class);
    public static final Property<Date> OFFLINE_REMOVED_AT = Property.of(TrackProperty.class, Date.class);
    public static final Property<Date> OFFLINE_REQUESTED_AT = Property.of(TrackProperty.class, Date.class);
}
