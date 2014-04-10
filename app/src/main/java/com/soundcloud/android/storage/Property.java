package com.soundcloud.android.storage;

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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Binding binding = (Binding) o;

            if (property != null ? !property.equals(binding.property) : binding.property != null) return false;
            if (value != null ? !value.equals(binding.value) : binding.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = property != null ? property.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
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
