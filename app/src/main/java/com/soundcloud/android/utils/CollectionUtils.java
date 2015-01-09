package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import java.util.List;

// Move this to core-utils once that module is fully integrated
public final class CollectionUtils {

    public static <T extends PropertySetSource> List<PropertySet> toPropertySets(List<T> collection) {
        return Lists.transform(collection, new Function<T, PropertySet>() {
            @Override
            public PropertySet apply(T source) {
                return source.toPropertySet();
            }
        });
    }

    private CollectionUtils() {
        // no instances
    }
}
