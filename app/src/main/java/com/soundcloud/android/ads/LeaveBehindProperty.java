package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.Property;

public class LeaveBehindProperty extends VisualAdProperty {
    public static final Property<String> LEAVE_BEHIND_URN = Property.of(LeaveBehindProperty.class, String.class);
    public static final Property<Boolean> META_AD_COMPLETED = Property.of(LeaveBehindProperty.class, Boolean.class);
    public static final Property<Boolean> META_AD_CLICKED = Property.of(LeaveBehindProperty.class, Boolean.class);
    public static final Property<String> AD_URN = Property.of(LeaveBehindProperty.class, String.class);
    public static final Property<Urn> AUDIO_AD_TRACK_URN = Property.of(LeaveBehindProperty.class, Urn.class);
}

