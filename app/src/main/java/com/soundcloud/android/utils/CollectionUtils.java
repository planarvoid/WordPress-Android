package com.soundcloud.android.utils;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// Move this to core-utils once that module is fully integrated
public final class CollectionUtils {

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
        return Strings.joinOn(delimiter).join(idStrings);
    }

    /**
     * Determines whether two iterators contain equal elements in the same order.
     * More specifically, this method returns {@code true} if {@code iterator1}
     * and {@code iterator2} contain the same number of elements and every element
     * of {@code iterator1} is equal to the corresponding element of
     * {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have
     * been advanced some number of elements forward.
     * <p>
     * Based on https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/Iterables.java
     */
    public static boolean elementsEqual(
            Iterator<?> iterator1, Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if (!MoreObjects.equal(o1, o2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }

    /**
     * Determines whether two iterables contain equal elements in the same order.
     * More specifically, this method returns {@code true} if {@code iterable1}
     * and {@code iterable2} contain the same number of elements and every element
     * of {@code iterable1} is equal to the corresponding element of
     * {@code iterable2}.
     * <p>
     * Based on https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/Iterables.java
     */
    public static boolean elementsEqual(
            Iterable<?> iterable1, Iterable<?> iterable2) {
        if (iterable1 instanceof Collection && iterable2 instanceof Collection) {
            Collection<?> collection1 = (Collection<?>) iterable1;
            Collection<?> collection2 = (Collection<?>) iterable2;
            if (collection1.size() != collection2.size()) {
                return false;
            }
        }
        return elementsEqual(iterable1.iterator(), iterable2.iterator());
    }

    private CollectionUtils() {
        // no instances
    }
}
