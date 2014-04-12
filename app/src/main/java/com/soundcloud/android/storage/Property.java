package com.soundcloud.android.storage;

import com.google.common.base.Objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Property represents a uniquely identifiable, typed, storeable and parcelable value of an entity in
 * the SoundCloud business domain. Examples include: a track title, a playlist URN, number of likes, etc.
 * <p/>
 * Properties can be bound to concrete values via bindings, accessible through the {@link #bind} method.
 * These bindings can then be grouped together in {@link com.soundcloud.android.storage.PropertySet}s to represent full
 * entities (such as tracks or users) for further consumption.
 *
 * @param <T>
 */
public final class Property<T> implements Parcelable {

    public static final Creator<Property> CREATOR = new Creator<Property>() {
        @Override
        public Property createFromParcel(Parcel source) {
            final Class<?> type;
            try {
                type = Class.forName(source.readString());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to restore parceled property type\n" + e);
            }
            final int id = source.readInt();
            return new Property(type, id);
        }

        @Override
        public Property[] newArray(int size) {
            return new Property[size];
        }
    };

    // we need to be able to identify properties even after being parceled, so we cannot use
    // reference equivalence to define equality, and the type itself is not sufficient.
    private static int runningPropertyId;

    final Class<T> type;
    final int id;

    public static <T> Property<T> of(Class<T> type) {
        return new Property<T>(type, runningPropertyId++);
    }

    Property(Class<T> type, int id) {
        this.type = type;
        this.id = id;
    }

    public Binding<T> bind(T value) {
        return new Binding<T>(this, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Property property = (Property) o;
        return id == property.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type.getCanonicalName());
        dest.writeInt(id);
    }

    @Override
    public String toString() {
        return "Property.of(" + this.type.getSimpleName() + ".class)@" + hashCode();
    }

    /**
     * Binds a given value to a {@link com.soundcloud.android.storage.Property}.
     * Values MUST be either parcelable or serializable.
     * <p/>
     * Bindings are never created manually, and instead are added to and contained in
     * {@link com.soundcloud.android.storage.PropertySet}s.
     *
     * @param <T>
     */
    public static final class Binding<T> implements Parcelable {

        public static final Creator<Binding> CREATOR = new Creator<Binding>() {
            @Override
            public Binding createFromParcel(Parcel source) {
                Property prop = source.readParcelable(getClass().getClassLoader());
                Object value = source.readValue(getClass().getClassLoader());
                return new Binding(prop, value);
            }

            @Override
            public Binding[] newArray(int size) {
                return new Binding[size];
            }
        };

        final Property<T> property;
        final T value;

        Binding(Property<T> property, T value) {
            this.property = property;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Binding binding = (Binding) o;
            return Objects.equal(this.property, binding.property) && Objects.equal(this.value, binding.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(property, value);
        }

        @Override
        public String toString() {
            return property.toString() + "=>[" + value + "]";
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(property, 0);
            dest.writeValue(value);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
