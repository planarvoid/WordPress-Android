package com.soundcloud.android.utils;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;

public class GuavaFunctions {
    private static final Function<PropertySetSource, PropertySet> PROP_SET_SOURCE_TO_PROP_SET = new Function<PropertySetSource, PropertySet>() {
        @Override
        public PropertySet apply(PropertySetSource propertySetSource) {
            return propertySetSource.toPropertySet();
        }
    };

    private static final Function<Urn, String> URN_TO_STRING = new Function<Urn, String>() {
        @Override
        public String apply(Urn input) {
            return input.toString();
        }
    };

    public static Function<PropertySetSource, PropertySet> toPropertySet() {
        return PROP_SET_SOURCE_TO_PROP_SET;
    }

    public static Function<Urn, String> urnToString() {
        return URN_TO_STRING;
    }
}
