package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.support.v4.util.SimpleArrayMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Implements(SimpleArrayMap.class)
public class ShadowArrayMap {

    private final Map map = new HashMap();

    @Implementation
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    @Implementation
    public Object get(Object key) {
        return map.get(key);
    }

    @Implementation
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Implementation
    public int size() {
        return map.size();
    }

    @Implementation
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Implementation
    public Set keySet() {
        return map.keySet();
    }

    @Implementation
    public Set<Map.Entry> entrySet() {
        return map.entrySet();
    }

    @Implementation
    public Collection values() {
        return map.values();
    }

    @Implementation
    public boolean equals(Object o) {
        return o.equals(map);
    }

    @Implementation
    public int hashCode() {
        return map.hashCode();
    }

    @Implementation
    public Object remove(Object key) {
        return map.remove(key);
    }
}
