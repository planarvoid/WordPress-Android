package com.soundcloud.android.ads;

import com.soundcloud.propeller.Property;
import com.soundcloud.propeller.PropertySet;

public class AdsForTrackProperty {
    public static final Property<PropertySet> AUDIO_AD = Property.of(AdsForTrackProperty.class, PropertySet.class);
    public static final Property<PropertySet> INTERSTITIAL_AD = Property.of(AdsForTrackProperty.class, PropertySet.class);
}
