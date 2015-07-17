package com.soundcloud.android.utils.cache;

public abstract class Cache<K, V> {

    /**
     * Creates an LRU cache with strongly reachable keys and values.
     */
    public static <K, V> Cache<K, V> withStrongValues(int maxSize) {
        return new StrongValuesCache<>(maxSize);
    }

    /**
     * Creates an LRU cache with strongly reachable keys and softly reachable values.
     */
    public static <K, V> Cache<K, V> withSoftValues(int maxSize) {
        return new SoftValuesCache<>(maxSize);
    }

    public abstract Cache<K, V> put(K key, V value);

    public abstract V get(K key);

    public abstract V get(K key, ValueProvider<K, V> valueProvider);

    public abstract void clear();

    public abstract int size();

    public abstract int hitCount();

    public abstract int missCount();

    public interface ValueProvider<K, V> {
        V get(K key) throws Exception;
    }
}
