package com.soundcloud.android.utils.cache;

import java.lang.ref.SoftReference;

/**
 * An LRU cache implementation that uses soft values.
 *
 * @see {@link java.lang.ref.SoftReference}
 */
class SoftValuesCache<K, V> extends Cache<K, V> {

    private final StrongValuesCache<K, SoftReference<V>> cache;

    SoftValuesCache(int maxSize) {
        cache = new StrongValuesCache<>(maxSize);
    }

    /**
     * Caches the given value using a {@link SoftReference}
     */
    @Override
    public Cache<K, V> put(K key, V value) {
        cache.put(key, new SoftReference<>(value));
        return this;
    }

    /**
     * @return the value for this key or null if no mapping found or
     * the reference has been released
     */
    @Override
    public V get(K key) {
        final SoftReference<V> softValue = cache.get(key);
        if (softValue == null) {
            return null;
        }
        return softValue.get();
    }

    /**
     * @see StrongValuesCache#get(Object, ValueProvider)
     */
    @Override
    public V get(K key, ValueProvider<K, V> valueProvider) {
        V value = get(key);
        if (value == null) {
            try {
                value = valueProvider.get(key);
                put(key, value);
            } catch (Exception e) {
                e.printStackTrace();
                value = null;
            }
        }
        return value;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public int hitCount() {
        return cache.hitCount();
    }

    @Override
    public int missCount() {
        return cache.missCount();
    }

    @Override
    public String toString() {
        return cache.toString();
    }
}
