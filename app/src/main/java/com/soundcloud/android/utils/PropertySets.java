package com.soundcloud.android.utils;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PropertySets {

    private static final Function<PropertySetSource, PropertySet> PROP_SET_SOURCE_TO_PROP_SET = new Function<PropertySetSource, PropertySet>() {
        @Override
        public PropertySet apply(PropertySetSource propertySetSource) {
            return propertySetSource.toPropertySet();
        }
    };

    @SuppressWarnings({"PMD.LooseCoupling"}) // we need ArrayList for Parceling
    public static ArrayList<Urn> extractUrns(List<PropertySet> entities) {
        ArrayList<Urn> urns = new ArrayList<>(entities.size());
        for (PropertySet propertySet : entities){
            urns.add(propertySet.get(EntityProperty.URN));
        }
        return urns;
    }

    @SuppressFBWarnings(
            value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
            justification = "Since there is no way to recover from a null source, we want to fail fast")
    public static <T extends PropertySetSource> List<PropertySet> toPropertySets(List<T> collection) {
        return transform(collection, new Function<T, PropertySet>() {
            @Override
            public PropertySet apply(T source) {
                return source.toPropertySet();
            }
        });
    }

    public static <T extends PropertySetSource> List<PropertySet> toPropertySets(T... items) {
        return toPropertySets(Arrays.asList(items));
    }

    public static Function<PropertySetSource, PropertySet> toPropertySet() {
        return PROP_SET_SOURCE_TO_PROP_SET;
    }

    private PropertySets() {
        // no instances
    }
}
