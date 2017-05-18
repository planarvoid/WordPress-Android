package com.soundcloud.java.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MultiMap<K, V> {

    MultiMap<K, V> put(K key, V value);

    MultiMap<K, V> putAll(K key, Iterable<V> values);

    Collection<V> get(K key);

    Set<K> keySet();

    Map<K, ? extends Collection<V>> toMap();

    int size();

    boolean isEmpty();
}
