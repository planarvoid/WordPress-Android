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

    public static final int ASC = 1;
    public static final int DESC = -1;

    private final Property<T> property;
    private final int order;

    public PropertySetComparator(Property<T> property) {
        this(property, ASC);
    }

    public PropertySetComparator(Property<T> property, int order) {
        this.property = property;
        this.order = order;
    }

    @Override
    public int compare(PropertySet lhs, PropertySet rhs) {
        return order * lhs.get(property).compareTo(rhs.get(property));
    }
}
