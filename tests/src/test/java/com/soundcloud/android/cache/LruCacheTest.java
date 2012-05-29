package com.soundcloud.android.cache;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings({"UnnecessaryBoxing"})
public class LruCacheTest {
    @Test
    public void testPut() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        Integer key = Integer.valueOf(1);
        Integer value = Integer.valueOf(3);
        cache.put(key, value);
        assertEquals(value, cache.get(key));
    }

    @Test
    public void testTracingInUsedObject() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        Integer key = Integer.valueOf(1);
        Integer value = Integer.valueOf(3);
        cache.put(key, value);
        for (int i = 0; i < 3; ++i) {
            cache.put(i + 10, i * i);
        }
        System.gc();
        assertEquals(value, cache.get(key));
    }

    @Test
    public void testLruAlgorithm() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        cache.put(0, Integer.valueOf(0));
        for (int i = 0; i < 3; ++i) {
            cache.put(i + 1, i * i);
            cache.get(0);
        }
        System.gc();
        assertEquals(Integer.valueOf(0), cache.get(0));
    }

    private static final int TEST_COUNT = 10000;

    static class Accessor extends Thread {
        private final LruCache<Integer,Integer> mMap;

        public Accessor(LruCache<Integer, Integer> map) {
            mMap = map;
        }

        @Override
        public void run() {
            for (int i = 0; i < TEST_COUNT; ++i) {
                mMap.get(i % 2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentAccess() throws Exception {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(4);
        cache.put(0, 0);
        cache.put(1, 1);
        Accessor accessor[] = new Accessor[4];
        for (int i = 0; i < accessor.length; ++i) {
            accessor[i] = new Accessor(cache);
        }
        for (Accessor anAccessor : accessor) {
            anAccessor.start();
        }
        for (Accessor anAccessor : accessor) {
            anAccessor.join();
        }
    }
}