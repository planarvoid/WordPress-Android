package com.soundcloud.java.test;

import java.util.HashMap;
import java.util.Map;

public final class TestMaps {

    public static <K, V> Map<K, V> from(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new AssertionError("Can only create map from even number or arguments");
        }
        if (keyValuePairs.length == 0) {
            return new HashMap<>();
        }
        final Class<?> keyType = keyValuePairs[0].getClass();
        final Class<?> valType = keyValuePairs[1].getClass();

        final HashMap<K, V> map = new HashMap<>(keyValuePairs.length);
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            Object k = keyValuePairs[i];
            Object v = keyValuePairs[i + 1];
            if (!k.getClass().equals(keyType)) {
                throw new ClassCastException("Type of map key doesn't match at index " + i);
            }
            if (!v.getClass().equals(valType)) {
                throw new ClassCastException("Type of map value doesn't match at index " + i + 1);
            }
            map.put((K) k, (V) v);
        }

        return map;
    }

    private TestMaps() {
        // no instances
    }
}
