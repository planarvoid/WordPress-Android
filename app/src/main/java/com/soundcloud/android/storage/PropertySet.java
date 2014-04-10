package com.soundcloud.android.storage;

import com.google.common.base.Objects;
import org.jetbrains.annotations.Nullable;

import android.util.SparseArray;

public final class PropertySet {

    private final SparseArray<Property.Binding<?>> table;

    public static PropertySet create(int capacity) {
        return new PropertySet(capacity);
    }

    public static PropertySet from(Property.Binding<?>... bindings) {
        final PropertySet propertySet = PropertySet.create(bindings.length);
        for (Property.Binding<?> binding : bindings) {
            propertySet.table.put(binding.property.hashCode(), binding);
        }
        return propertySet;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertySet that = (PropertySet) o;

        for (int i = 0; i < table.size(); i++) {
            final Property.Binding<?> thisBinding = table.get(table.keyAt(i));
            final Object value = that.get(thisBinding.property);
            // null means the property didn't exist in the other set
            if (value == null || !value.equals(thisBinding.value)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final Property.Binding<?>[] bindings = new Property.Binding[table.size()];
        for (int i = 0; i < table.size(); i++) {
            bindings[i] = table.get(table.keyAt(i));
        }
        return Objects.hashCode(bindings);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PropertySet{");
        for (int i = 0; i < table.size(); i++) {
            sb.append(table.get(table.keyAt(i)).toString());
            sb.append(';');
        }
        sb.append('}');
        return sb.toString();
    }
}
