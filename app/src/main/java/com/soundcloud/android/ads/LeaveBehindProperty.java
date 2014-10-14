package com.soundcloud.android.ads;

import com.soundcloud.propeller.Property;

public class LeaveBehindProperty extends VisualAdProperty {
    public static final Property<String> LEAVE_BEHIND_URN = Property.of(LeaveBehindProperty.class, String.class);
    public static final Property<Boolean> META_AD_COMPLETED = Property.of(LeaveBehindProperty.class, Boolean.class);
    public static final Property<Boolean> META_AD_CLICKED = Property.of(LeaveBehindProperty.class, Boolean.class);
    public static final Property<Boolean> IS_INTERSTITIAL = Property.of(LeaveBehindProperty.class, Boolean.class);
}
