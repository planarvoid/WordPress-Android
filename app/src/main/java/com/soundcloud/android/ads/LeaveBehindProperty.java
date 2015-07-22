package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

public class LeaveBehindProperty extends AdOverlayProperty {
    public static final Property<String> LEAVE_BEHIND_URN = Property.of(LeaveBehindProperty.class, String.class);
    public static final Property<Urn> AUDIO_AD_TRACK_URN = Property.of(LeaveBehindProperty.class, Urn.class);
}

