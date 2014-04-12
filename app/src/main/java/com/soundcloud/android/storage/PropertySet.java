package com.soundcloud.android.storage;

import com.google.common.base.Objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

/**
 * A PropertySet represents a set of {@link com.soundcloud.android.storage.Property.Binding}s.
 * It can be understood as a fluent way of representing a SoundCloud business object such as
 * a track or user in a type safe fashion, without having to define various model classes
 * representing different combinations of fields.
 */
public final class PropertySet implements Parcelable {

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

    private void addBinding(Property.Binding<?> binding) {
        table.put(binding.property.id, binding);
    }

    public <T> void add(Property<T> property, T value) {
        addBinding(property.bind(value));
    }

    public <V> V get(Property<V> property) {
        final Property.Binding<?> binding = table.get(property.hashCode());
        if (binding != null) {
            return property.type.cast(binding.value);
        } else {
            throw new AssertionError("Attempt to read a property that doesn't exist: " + property);
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
        StringBuilder sb = new StringBuilder("PropertySet{");
        for (int i = 0; i < table.size(); i++) {
            sb.append(table.get(table.keyAt(i)).toString());
            sb.append(';');
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
