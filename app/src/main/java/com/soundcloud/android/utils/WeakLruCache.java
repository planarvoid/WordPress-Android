package com.soundcloud.android.utils;

import com.soundcloud.android.utils.BetterLruCache.ValueProvider;

import java.lang.ref.WeakReference;

public class WeakLruCache<K, V> {

    private final BetterLruCache<K, WeakReference<V>> cache;

    public WeakLruCache(int maxSize) {
        cache = new BetterLruCache<>(maxSize);
    }

    /**
     * Caches the given value using a {@link WeakReference}
     */
    public void put(K key, V value) {
        cache.put(key, new WeakReference<>(value));
    }

    /**
     * @return the value for this key or null if no mapping found or
     * the weak reference has been released
     */
    public V get(K key) {
        final WeakReference<V> weakValue = cache.get(key);
        if (weakValue == null) {
            return null;
        }
        return weakValue.get();
    }

    /**
     * @see BetterLruCache#get(Object, ValueProvider)
     */
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
    public String toString() {
        return cache.toString();
    }
}
