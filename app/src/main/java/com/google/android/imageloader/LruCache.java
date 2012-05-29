package com.google.android.imageloader;

import java.util.LinkedHashMap;

/**
 * A LRU cache for holding image data or meta-data.
 */
class LruCache<K, V> extends LinkedHashMap<K, V> {

    private static final int INITIAL_CAPACITY = 32;

    // Hold information for at least a few pages full of thumbnails.
    private static final int MAX_CAPACITY = 256;

    private static final float LOAD_FACTOR = 0.75f;

    public LruCache() {
        super(INITIAL_CAPACITY, LOAD_FACTOR, true);
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > MAX_CAPACITY;
    }
}