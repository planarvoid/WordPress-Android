package com.soundcloud.android.storage;

public final class Property<T> {

    final Class<T> type;

    public static <T> Property<T> of(Class<T> type) {
        return new Property<T>(type);
    }

    private Property(Class<T> type) {
        this.type = type;
    }

    Binding<T> bind(T value) {
        return new Binding<T>(this, value);
    }

    static final class Binding<T> {
        final Property<T> property;
        final T value;

        private Binding(Property<T> property, T value) {
            this.property = property;
            this.value = value;
        }
    }
}
