package com.soundcloud.android.cache;

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
    private final HashMap<K, V> lruMap;
    private long lruHits, requests;

    /**
     * @param capacity max capacity for the LRU cache
     */
    public LruCache(final int capacity) {
        lruMap = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized V put(K key, V value) {
        return lruMap.put(key, value);
    }

    public synchronized V get(K key) {
        requests++;

        V value = lruMap.get(key);
        if (value != null) {
            lruHits++;
            return value;
        }
        return null;
    }

    public synchronized void clear() {
        lruMap.clear();
        lruHits = requests = 0;
    }

    public synchronized boolean containsKey(K key) {
        return lruMap.containsKey(key);
    }

    public synchronized void remove(K key) {
        lruMap.remove(key);
    }

    public synchronized String toString() {
        return "LruCache{lru: " + lruMap.size() +
                " lru ratio: " + String.format("%.2f", lruHits / (double) (requests)) +
                "}";
    }
}
