package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.model.ScModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (C) 2009 The Android Open Source Project
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.3.3_r1/com/android/camera/gallery/LruCache.java#LruCache">
 * com/android/camera/gallery/LruCache.java
 * </a>
 */
@Deprecated // use our own Cache class
class ModelCache<V extends ScModel> {

    private final Map<Long, V> lruMap;
    private final Object mapLock;
    private final AtomicInteger lruHits, requests;

    /**
     * @param capacity max capacity for the LRU cache
     */
    ModelCache(final int capacity) {
        mapLock = new Object();
        lruHits = new AtomicInteger(0);
        requests = new AtomicInteger(0);
        lruMap = new LinkedHashMap<Long, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public V put(V resource) {
        return resource != null ? put(resource.getId(), resource) : null;
    }

    public V put(Long key, V value) {
        synchronized (mapLock) {
            return lruMap.put(key, value);
        }
    }

    public synchronized V get(Long key) {
        requests.incrementAndGet();

        V value;
        synchronized (mapLock) {
            value = lruMap.get(key);
        }
        if (value != null) {
            lruHits.incrementAndGet();
        }
        return value;
    }

    public void clear() {
        synchronized (mapLock) {
            lruMap.clear();
        }
        lruHits.set(0);
        requests.set(0);
    }

    public boolean containsKey(Long key) {
        synchronized (mapLock) {
            return lruMap.containsKey(key);
        }
    }

    public void remove(Long key) {
        synchronized (mapLock) {
            lruMap.remove(key);
        }
    }

    public String toString() {
        return "LruCache{lru: " + lruMap.size() +
                " lru ratio: " + String.format("%.2f", lruHits.doubleValue() / requests.doubleValue()) +
                "}";
    }

}
