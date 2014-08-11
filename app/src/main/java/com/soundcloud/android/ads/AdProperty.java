package com.soundcloud.android.ads;

import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.Property;

import android.net.Uri;

public class AdProperty {
    public static final Property<Uri> CLICK_THROUGH_LINK = Property.of(Uri.class);
    public static final Property<Uri> ARTWORK = Property.of(Uri.class);
    public static final Property<TrackUrn> MONETIZABLE_TRACK_URN =  Property.of(TrackUrn.class);
    public static final Property<String> MONETIZABLE_TRACK_TITLE =  Property.of(String.class);
    public static final Property<String> MONETIZABLE_TRACK_CREATOR =  Property.of(String.class);
    public static final Property<String> DEFAULT_TEXT_COLOR = Property.of(String.class);
    public static final Property<String> DEFAULT_BACKGROUND_COLOR = Property.of(String.class);
    public static final Property<String> PRESSED_TEXT_COLOR = Property.of(String.class);
    public static final Property<String> PRESSED_BACKGROUND_COLOR = Property.of(String.class);
    public static final Property<String> FOCUSED_TEXT_COLOR = Property.of(String.class);
    public static final Property<String> FOCUSED_BACKGROUND_COLOR = Property.of(String.class);
}
