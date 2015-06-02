package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    public static <T> Collection<T> add(Collection<T> items, Collection<T>... collectionsToAdd) {
        final ArrayList<T> result = new ArrayList<>(items);
        for (Collection<T> itemsToAdd : collectionsToAdd) {
            result.addAll(itemsToAdd);
        }
        return result;
    }

    public static <T> Collection<T> addAll(Collection<T> items, Iterable<T> iterable) {
        for (T item : iterable){
            items.add(item);
        }
        return items;
    }

    public static <T> Collection<T> subtract(Collection<T> items, Collection<T>... collectionsToSubtract) {
        final ArrayList<T> result = new ArrayList<>(items);
        for (Collection<T> itemsToSubtract : collectionsToSubtract) {
            result.removeAll(itemsToSubtract);
        }
        return result;
    }

    @SuppressWarnings({"PMD.LooseCoupling"}) // we need ArrayList for Parceling
    public static ArrayList<Urn> extractUrnsFromEntities(List<PropertySet> entities) {
        ArrayList<Urn> urns = new ArrayList<>(entities.size());
        for (PropertySet propertySet : entities){
            urns.add(propertySet.get(EntityProperty.URN));
        }
        return urns;
    }

    public static ArrayList<String> urnsToStrings(List<Urn> urns){
        final ArrayList<String> urnStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            urnStrings.add(urn.toString());
        }
        return urnStrings;
    }

    public static String urnsToJoinedIds(List<Urn> urns, String delimiter){
        final ArrayList<String> idStrings = new ArrayList<>(urns.size());
        for (Urn urn : urns) {
            idStrings.add(String.valueOf(urn.getNumericId()));
        }
        return Joiner.on(delimiter).join(idStrings);
    }

    private CollectionUtils() {
        // no instances
    }
}
