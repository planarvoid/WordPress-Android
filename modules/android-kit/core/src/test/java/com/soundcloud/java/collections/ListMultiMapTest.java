package com.soundcloud.java.collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.java.test.EqualsTester;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ListMultiMapTest {

    private ListMultiMap<String, Integer> multiMap = new ListMultiMap<>();

    @Test
    public void shouldStoreSingleKeySingleValue() {
        multiMap.put("one", 1);
        assertThat(multiMap.get("one")).containsOnly(1);
    }

    @Test
    public void shouldStoreSingleKeyMultipleValues() {
        multiMap.put("one", 1);
        multiMap.put("one", 11);
        assertThat(multiMap.get("one")).containsExactly(1, 11);
    }

    @Test
    public void shouldStoreMultupleKeysWithSingleValues() {
        multiMap.put("one", 1);
        multiMap.put("two", 2);
        assertThat(multiMap.get("one")).containsOnly(1);
        assertThat(multiMap.get("two")).containsOnly(2);
    }

    @Test
    public void shouldStoreMultipleValuesUnderGivenKeyForCollection() {
        multiMap.putAll("key", asList(1, 2));
        assertThat(multiMap.get("key")).containsExactly(1, 2);
    }

    @Test
    public void shouldAppendMultipleValuesUnderGivenKeyForCollection() {
        multiMap.put("key", 0);
        multiMap.putAll("key", asList(1, 2));
        assertThat(multiMap.get("key")).containsExactly(0, 1, 2);
    }

    @Test
    public void shouldStoreMultipleValuesUnderGivenKeyForIterable() {
        multiMap.putAll("key", new IntIterable(asList(1, 2)));
        assertThat(multiMap.get("key")).containsExactly(1, 2);
    }

    @Test
    public void shouldAppendMultipleValuesUnderGivenKeyForIterable() {
        multiMap.put("key", 0);
        multiMap.putAll("key", new IntIterable(asList(1, 2)));
        assertThat(multiMap.get("key")).containsExactly(0, 1, 2);
    }

    @Test
    public void shouldStoreEmptyCollectionsViaPutAll() {
        multiMap.putAll("key", new IntIterable(Collections.<Integer>emptyList()));
        assertThat(multiMap.get("key")).isEmpty();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableCollectionViaGet() {
        multiMap.get("key").add(1);
    }

    @Test
    public void shouldReturnEmptyListForAbsentKeys() {
        assertThat(multiMap.get("key")).isEmpty();
    }

    @Test
    public void shouldDefineSize() {
        assertThat(multiMap.size()).isEqualTo(0);
        multiMap.put("one", 1);
        assertThat(multiMap.size()).isEqualTo(1);
        multiMap.put("two", 2);
        assertThat(multiMap.size()).isEqualTo(2);
    }

    @Test
    public void shouldDefineIsEmpty() {
        assertThat(multiMap.isEmpty()).isTrue();
        multiMap.put("one", 1);
        assertThat(multiMap.isEmpty()).isFalse();
    }

    @Test
    public void shouldConvertToEmptyMap() {
        Map<String, List<Integer>> map = multiMap.toMap();
        assertThat(map).isNotNull();
        assertThat(map).isEmpty();
    }

    @Test
    public void shouldConvertToMap() {
        multiMap.put("one", 1);
        multiMap.put("two", 2);
        multiMap.put("two", 22);
        Map<String, List<Integer>> map = multiMap.toMap();
        assertThat(map).hasSize(2);
        assertThat(map).containsKeys("one", "two");
        assertThat(map.get("one")).containsExactly(1);
        assertThat(map.get("two")).containsExactly(2, 22);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        multiMap.toMap().remove("key");
    }

    @Test
    public void shouldCreateMultiMapFromMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("one", new ArrayList<Integer>());
        map.get("one").add(1);
        map.put("two", new ArrayList<Integer>());
        map.get("two").add(2);
        map.get("two").add(22);

        MultiMap<String, Integer> multiMap = new ListMultiMap<>(map);
        assertThat(multiMap.size()).isEqualTo(map.size());
        assertThat(multiMap.get("one")).containsExactly(1);
        assertThat(multiMap.get("two")).containsExactly(2, 22);
    }

    @Test
    public void shouldProvideKeySet() {
        assertThat(multiMap.keySet()).isEmpty();
        multiMap.put("one", 1);
        multiMap.put("two", 2);
        multiMap.put("two", 22);
        assertThat(multiMap.keySet()).containsOnly("one", "two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void keySetIsUnmodifiable() {
        multiMap.put("one", 1);
        multiMap.keySet().remove("one");
    }

    @Test
    public void shouldDefineEqualsAndHashCode() {
        Map<String, List<Integer>> map1 = new HashMap<>();
        map1.put("one", new ArrayList<Integer>());
        map1.get("one").add(1);
        Map<String, List<Integer>> map2 = new HashMap<>();
        map2.put("two", new ArrayList<Integer>());
        map2.get("two").add(1);

        MultiMap<String, Integer> multiMap1 = new ListMultiMap<>(map1);
        MultiMap<String, Integer> multiMap2 = new ListMultiMap<>(map1);

        new EqualsTester()
                .addEqualityGroup(map1, map1)
                .addEqualityGroup(multiMap1, multiMap2)
                .addEqualityGroup(new ListMultiMap<>(map2))
                .testEquals();
    }

    @Test
    public void shouldNotAffectEqualityWhenGettingAbsentValues() {
        MultiMap<String, Integer> multiMap1 = new ListMultiMap<>();
        MultiMap<String, Integer> multiMap2 = new ListMultiMap<>();

        multiMap1.get("key"); // this used to create an entry for "key"

        new EqualsTester()
                .addEqualityGroup(multiMap1, multiMap2)
                .testEquals();
    }

    @Test
    public void toMapShouldNotBeAffectedByGettingAbsentKeys() {
        MultiMap<String, Integer> multiMap1 = new ListMultiMap<>();
        MultiMap<String, Integer> multiMap2 = new ListMultiMap<>();

        multiMap1.get("key"); // this used to create an entry for "key"

        new EqualsTester()
                .addEqualityGroup(multiMap1.toMap(), multiMap2.toMap())
                .testEquals();
    }

    private static class IntIterable implements Iterable<Integer> {
        private final Collection<Integer> wrappedCollection;

        private IntIterable(Collection<Integer> wrappedCollection) {
            this.wrappedCollection = wrappedCollection;
        }

        @Override
        public Iterator<Integer> iterator() {
            return wrappedCollection.iterator();
        }
    }
}
