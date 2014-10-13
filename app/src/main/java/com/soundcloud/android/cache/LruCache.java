package com.soundcloud.android.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
public class LruCache<K, V> {
    private final Map<K, V> lruMap;
    private final Map<K, Entry<K, V>> softmap = new HashMap<>();

    private ReferenceQueue<V> queue = new ReferenceQueue<>();

    private long lruHits, softHits, requests, softRequests;


    /**
     * 2 level cache - LRU (bound to capacity) + softreference map (unbound)
     *
     * @param capacity max capacity for the LRU cache
     */
    public LruCache(final long capacity) {
        lruMap = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    private static class Entry<K, V> extends SoftReference<V> {
        K mKey;

        public Entry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            mKey = key;
        }
    }

    private void cleanUpSoftMap() {
        Entry<K, V> entry = (Entry<K, V>) queue.poll();
        while (entry != null) {
            softmap.remove(entry.mKey);
            entry = (Entry<K, V>) queue.poll();
        }
    }

    public synchronized V put(K key, V value) {
        cleanUpSoftMap();
        lruMap.put(key, value);
        Entry<K, V> entry = softmap.put(key, new Entry<>(key, value, queue));
        return entry == null ? null : entry.get();
    }

    public synchronized V get(K key) {
        requests++;

        cleanUpSoftMap();
        V value = lruMap.get(key);
        if (value != null) {
            lruHits++;
            return value;
        }

        softRequests++;

        Entry<K, V> entry = softmap.get(key);
        if (entry != null) {
            V v = entry.get();
            if (v != null) {
                softHits++;
            }
            return v;
        } else {
            return null;
        }
    }

    public synchronized void clear() {
        lruMap.clear();
        softmap.clear();
        queue = new ReferenceQueue<>();
        softHits = lruHits = requests = 0;
    }

    public boolean containsKey(K key) {
        return lruMap.containsKey(key);
    }

    public void remove(K key) {
        lruMap.remove(key);
        softmap.remove(key);
    }

    public String toString() {
        return "LruCache{lru: " + lruMap.size() + " soft: " + softmap.size() +
                " lru ratio: " + String.format("%.2f", lruHits / (double) (requests)) +
                " soft ratio: " + String.format("%.2f", softHits / (double) (softRequests)) +
                "}";
    }
}
