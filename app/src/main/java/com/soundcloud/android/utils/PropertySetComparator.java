package com.soundcloud.android.utils;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;

import java.util.Comparator;

/**
 * Compares two property sets based on a given property. Assumes that the two sets always contain bindings for
 * that property.
 *
 * @param <T> the {@link Property}'s type parameter (must be {@link Comparable})
 */
public class PropertySetComparator<T extends Comparable<T>> implements Comparator<PropertySet> {

    private final Property<T> property;

    public PropertySetComparator(Property<T> property) {
        this.property = property;
    }

    @Override
    public int compare(PropertySet lhs, PropertySet rhs) {
        return lhs.get(property).compareTo(rhs.get(property));
    }
}
