package com.soundcloud.android.utils;

import android.support.v4.util.LruCache;

public class BetterLruCache<K, V> extends LruCache<K, V> {

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public BetterLruCache(int maxSize) {
        super(maxSize);
    }

    /**
     * Returns the value under the given key if present, or falls back to the given
     * {@link com.soundcloud.android.utils.BetterLruCache.ValueProvider} to create
     * and cache a new value. This method provides a simple substitute for the conventional
     * "if cached, return; otherwise create, cache and return" pattern.
     * Returns null if valueProvider returns null or throws.
     */
    public V get(K key, ValueProvider<K, V> valueProvider) {
        V value = super.get(key);
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

    public interface ValueProvider<K, V> {
        V get(K key) throws Exception;
    }
}
