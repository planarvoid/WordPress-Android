package com.soundcloud.android.storage;

import org.jetbrains.annotations.Nullable;

import android.util.SparseArray;

public final class PropertySet {

    private final SparseArray<Property.Binding<?>> table;

    public static PropertySet create(int capacity) {
        return new PropertySet(capacity);
    }

    private PropertySet(int capacity) {
        table = new SparseArray<Property.Binding<?>>(capacity);
    }

    public <T> void add(Property<T> property, T value) {
        table.put(property.hashCode(), property.bind(value));
    }

    @Nullable
    public <V> V get(Property<V> property) {
        final Property.Binding<?> binding = table.get(property.hashCode());
        if (binding != null) {
            return property.type.cast(binding.value);
        } else {
            return null;
        }
    }
}
