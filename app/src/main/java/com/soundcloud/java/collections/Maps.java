package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Function;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Todo : This should probably go in Android kit with the rest of the collections helpers
public final class Maps {

    public static <K,V> Map<K,V> asMap(Collection<V> collection, Function<V,K> function) {
        Map<K,V> map = new HashMap<>();
        for (V val : collection) {
            map.put(function.apply(val), val);
        }
        return map;
    }
}
