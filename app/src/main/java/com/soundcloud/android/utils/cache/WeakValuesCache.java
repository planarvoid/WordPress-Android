package com.soundcloud.android.utils.cache;

import java.lang.ref.WeakReference;

class WeakValuesCache<K, V> extends Cache<K, V> {

    private final DefaultCache<K, WeakReference<V>> cache;

    WeakValuesCache(int maxSize) {
        cache = new DefaultCache<>(maxSize);
    }

    /**
     * Caches the given value using a {@link WeakReference}
     */
    @Override
    public Cache<K, V> put(K key, V value) {
        cache.put(key, new WeakReference<>(value));
        return this;
    }

    /**
     * @return the value for this key or null if no mapping found or
     * the weak reference has been released
     */
    @Override
    public V get(K key) {
        final WeakReference<V> weakValue = cache.get(key);
        if (weakValue == null) {
            return null;
        }
        return weakValue.get();
    }

    /**
     * @see DefaultCache#get(Object, ValueProvider)
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
