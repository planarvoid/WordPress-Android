package com.soundcloud.android.ads;

import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;

public final class AdFunctions {

    private AdFunctions() {}

    public static final Predicate<PropertySet> HAS_AD_URN = new Predicate<PropertySet>() {
        @Override
        public boolean apply(PropertySet input) {
            return input.contains(AdProperty.AD_URN);
        }
    };
}
