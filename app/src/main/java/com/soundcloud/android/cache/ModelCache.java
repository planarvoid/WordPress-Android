package com.soundcloud.android.cache;

import com.soundcloud.android.model.ScModel;

public class ModelCache<V extends ScModel> extends LruCache<Long, V> {
    /**
     * @param capacity max capacity for the LRU cache
     */
    public ModelCache(int capacity) {
        super(capacity);
    }

    public V put(V resource) {
        return resource != null ? put(resource.getId(), resource) : null;
    }

    @Override
    public synchronized V get(Long key) {
        return super.get(key);
    }

}
