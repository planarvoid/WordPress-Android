package com.soundcloud.java.collections;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListMultiMap<K, V> implements MultiMap<K, V> {

    // heuristic taken from Guava's Multimap impl
    private static final int DEFAULT_VALUES_PER_KEY = 3;

    private final HashMap<K, List<V>> internalMap;
    private final int expectedValuesPerKey;

    public ListMultiMap() {
        this(DEFAULT_VALUES_PER_KEY);
    }

    public ListMultiMap(int expectedValuesPerKey) {
        this(expectedValuesPerKey, new HashMap<K, List<V>>());
    }

    public ListMultiMap(Map<K, List<V>> map) {
        this(DEFAULT_VALUES_PER_KEY, map);
    }

    ListMultiMap(int expectedValuesPerKey, Map<K, List<V>> map) {
        this.expectedValuesPerKey = expectedValuesPerKey;
        this.internalMap = new HashMap<>(map);
    }

    @NotNull
    @Override
    public ListMultiMap<K, V> put(K key, V value) {
        internalGetMutableValues(key).add(value);
        return this;
    }

    @NotNull
    @Override
    public ListMultiMap<K, V> putAll(K key, Iterable<V> values) {
        Iterables.addAll(internalGetMutableValues(key), values);
        return this;
    }

    @NotNull
    @Override
    public List<V> get(K key) {
        List<V> values = internalMap.get(key);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    @NotNull
    private List<V> internalGetMutableValues(K key) {
        List<V> values = internalMap.get(key);
        if (values == null) {
            values = new ArrayList<>(expectedValuesPerKey);
            internalMap.put(key, values);
        }
        return values;
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(internalMap.keySet());
    }

    @NotNull
    @Override
    public Map<K, List<V>> toMap() {
        return Collections.unmodifiableMap(internalMap);
    }

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ListMultiMap
                && ((ListMultiMap) obj).internalMap.equals(internalMap);
    }

    @Override
    public int hashCode() {
        return internalMap.hashCode();
    }
}
