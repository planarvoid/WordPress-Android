package com.soundcloud.android.utils.cache;

import android.support.v4.util.LruCache;

class DefaultCache<K, V> extends Cache<K, V> {

    private final LruCache<K, V> cache;

    DefaultCache(int maxSize) {
        cache = new LruCache<>(maxSize);
    }

    @Override
    public Cache<K, V> put(K key, V value) {
        cache.put(key, value);
        return this;
    }

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * Returns the value under the given key if present, or falls back to the given
     * {@link DefaultCache.ValueProvider} to create
     * and cache a new value. This method provides a simple substitute for the conventional
     * "if cached, return; otherwise create, cache and return" pattern.
     * Returns null if valueProvider returns null or throws.
     */
    @Override
    public V get(K key, ValueProvider<K, V> valueProvider) {
        V value = cache.get(key);
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
        cache.evictAll();
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
