package com.soundcloud.android.ads;

import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.Property;

import android.net.Uri;

public class AdProperty {
    public static final Property<Uri> CLICK_THROUGH_LINK = Property.of(Uri.class);
    public static final Property<Uri> ARTWORK = Property.of(Uri.class);
    public static final Property<TrackUrn> MONETIZABLE_TRACK_URN =  Property.of(TrackUrn.class);
}
