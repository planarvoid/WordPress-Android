package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Move this to core-utils once that module is fully integrated
public final class CollectionUtils {

    @SuppressFBWarnings(
            value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
            justification = "Since there is no way to recover from a null source, we want to fail fast")
    public static <T extends PropertySetSource> List<PropertySet> toPropertySets(List<T> collection) {
        return Lists.transform(collection, new Function<T, PropertySet>() {
            @Override
            public PropertySet apply(T source) {
                return source.toPropertySet();
            }
        });
    }

    public static <T extends PropertySetSource> List<PropertySet> toPropertySets(T... items) {
        return toPropertySets(Arrays.asList(items));
    }

    public static ArrayList<Urn> extractUrnsFromPlayables(List<PropertySet> playables) {
        ArrayList<Urn> urns = new ArrayList<>(playables.size());
        for (PropertySet propertySet : playables){
            urns.add(propertySet.get(PlayableProperty.URN));
        }
        return urns;
    }

    private CollectionUtils() {
        // no instances
    }
}
