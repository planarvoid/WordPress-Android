package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

public class GuavaFunctions {
    public static final Function<PropertySetSource, PropertySet> PROP_SET_SOURCE_TO_PROP_SET = new Function<PropertySetSource, PropertySet>() {
        @Override
        public PropertySet apply(PropertySetSource propertySetSource) {
            return propertySetSource.toPropertySet();
        }
    };
}
