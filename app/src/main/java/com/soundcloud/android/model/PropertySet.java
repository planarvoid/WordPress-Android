package com.soundcloud.android.model;

import com.google.common.base.Objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.Iterator;

/**
 * A PropertySet represents a set of {@link Property.Binding}s.
 * It can be understood as a fluent way of representing a SoundCloud business object such as
 * a track or user in a type safe fashion, without having to define various model classes
 * representing different combinations of fields.
 */
public final class PropertySet implements Parcelable, Iterable<Property.Binding> {

    public static final Creator<PropertySet> CREATOR = new Creator<PropertySet>() {
        @Override
        public PropertySet createFromParcel(Parcel source) {
            final SparseArray sparseArray = source.readSparseArray(PropertySet.class.getClassLoader());
            return new PropertySet(sparseArray);
        }

        @Override
        public PropertySet[] newArray(int size) {
            return new PropertySet[size];
        }
    };

    private final SparseArray<Property.Binding<?>> table;

    public static PropertySet create(int capacity) {
        return new PropertySet(capacity);
    }

    public static PropertySet from(Property.Binding<?>... bindings) {
        final PropertySet propertySet = PropertySet.create(bindings.length);
        for (Property.Binding<?> binding : bindings) {
            propertySet.addBinding(binding);
        }
        return propertySet;
    }

    private PropertySet(int capacity) {
        table = new SparseArray<Property.Binding<?>>(capacity);
    }

    private PropertySet(SparseArray<Property.Binding<?>> sparseArray) {
        this.table = sparseArray;
    }

    /**
     * Merges all bindings from the given source set into this set. This mutates the set on which `merge` is invoked,
     * adding missing entries from the source set and overriding existing ones.
     *
     * @param propertySet the set whose bindings should be merged
     * @return this set, with all bindings from the source set added or replaced
     */
    public PropertySet merge(PropertySet propertySet) {
        for (Property.Binding<?> binding : propertySet) {
            addBinding(binding);
        }
        return this;
    }

    @Override
    public Iterator<Property.Binding> iterator() {
        return new Iterator<Property.Binding>() {

            private int currentIndex;

            @Override
            public boolean hasNext() {
                return currentIndex < table.size();
            }

            @Override
            public Property.Binding next() {
                return table.valueAt(currentIndex++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void addBinding(Property.Binding<?> binding) {
        table.put(binding.property.id, binding);
    }

    public <T> PropertySet put(Property<T> property, T value) {
        addBinding(property.bind(value));
        return this;
    }

    public <V> V get(Property<V> property) {
        final Property.Binding<?> binding = table.get(property.hashCode());
        if (binding == null) {
            throw new AssertionError("Attempt to read a property that doesn't exist: " + property);
        } else {
            return property.type.cast(binding.value);
        }
    }

    public <T> boolean contains(Property<T> property) {
        return table.indexOfKey(property.id) >= 0;
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

        if (table.size() != that.table.size()) {
            return false;
        }

        for (int i = 0; i < table.size(); i++) {
            final Property.Binding<?> thisBinding = table.get(table.keyAt(i));
            if (!that.contains(thisBinding.property) || !thisBinding.value.equals(that.get(thisBinding.property))) {
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
        StringBuilder sb = new StringBuilder("PropertySet{\n");
        for (int i = 0; i < table.size(); i++) {
            sb.append('\t');
            sb.append(table.get(table.keyAt(i)).toString());
            sb.append('\n');
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSparseArray((SparseArray) table);
    }
}
