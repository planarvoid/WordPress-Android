package com.soundcloud.reporting;

import com.soundcloud.java.objects.MoreObjects;

/**
 * DataPoints form the dimensions in some {@link Metric}. They can be textual or numeric.
 *
 * @param <T> the value type
 */
public final class DataPoint<T> {

    private final String key;
    private final T value;

    DataPoint(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public static DataPoint<Number> numeric(String key, Number value) {
        return new DataPoint<>(key, value);
    }

    public static DataPoint<String> string(String key, String value) {
        return new DataPoint<>(key, value);
    }

    public String key() {
        return key;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataPoint<?> that = (DataPoint<?>) o;
        return MoreObjects.equal(key, that.key) &&
                MoreObjects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(key, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
