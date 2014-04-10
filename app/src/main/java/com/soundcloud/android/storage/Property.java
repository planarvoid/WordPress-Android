package com.soundcloud.android.storage;

import com.google.common.base.Objects;

public final class Property<T> {

    final Class<T> type;

    public static <T> Property<T> of(Class<T> type) {
        return new Property<T>(type);
    }

    private Property(Class<T> type) {
        this.type = type;
    }

    public Binding<T> bind(T value) {
        return new Binding<T>(this, value);
    }

    public static final class Binding<T> {
        final Property<T> property;
        final T value;

        private Binding(Property<T> property, T value) {
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
    }

    @Override
    public String toString() {
        return "Property.of(" + this.type.getSimpleName() + ".class)@" + hashCode();
    }
}
