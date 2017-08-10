/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.java.collections;

import static com.soundcloud.java.collections.Iterators.advance;
import static com.soundcloud.java.collections.Iterators.get;
import static com.soundcloud.java.collections.Iterators.getLast;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;

/**
 * Unit test for {@code Iterators}.
 *
 * @author Kevin Bourrillion
 */
public class IteratorsTest {

    @Test
    public void testEmptyIterator() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("no exception thrown");
        } catch (NoSuchElementException ignored) {
        }
        try {
            iterator.remove();
            fail("no exception thrown");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testEmptyListIterator() {
        ListIterator<String> iterator = Iterators.emptyListIterator();
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasPrevious());
        assertEquals(0, iterator.nextIndex());
        assertEquals(-1, iterator.previousIndex());
        try {
            iterator.next();
            fail("no exception thrown");
        } catch (NoSuchElementException expected) {
        }
        try {
            iterator.previous();
            fail("no exception thrown");
        } catch (NoSuchElementException expected) {
        }
        try {
            iterator.remove();
            fail("no exception thrown");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            iterator.set("a");
            fail("no exception thrown");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            iterator.add("a");
            fail("no exception thrown");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testEmptyModifiableIterator() {
        Iterator<String> iterator = Iterators.emptyModifiableIterator();
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
        }
        try {
            iterator.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testSize0() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertEquals(0, Iterators.size(iterator));
    }

    @Test
    public void testSize1() {
        Iterator<Integer> iterator = Collections.singleton(0).iterator();
        assertEquals(1, Iterators.size(iterator));
    }

    @Test
    public void testSize_partiallyConsumed() {
        Iterator<Integer> iterator = asList(1, 2, 3, 4, 5).iterator();
        iterator.next();
        iterator.next();
        assertEquals(3, Iterators.size(iterator));
    }

    @Test
    public void test_contains_nonnull_yes() {
        Iterator<String> set = asList("a", null, "b").iterator();
        assertTrue(Iterators.contains(set, "b"));
    }

    @Test
    public void test_contains_nonnull_no() {
        Iterator<String> set = asList("a", "b").iterator();
        assertFalse(Iterators.contains(set, "c"));
    }

    @Test
    public void test_contains_null_yes() {
        Iterator<String> set = asList("a", null, "b").iterator();
        assertTrue(Iterators.contains(set, null));
    }

    @Test
    public void test_contains_null_no() {
        Iterator<String> set = asList("a", "b").iterator();
        assertFalse(Iterators.contains(set, null));
    }

    @Test
    public void testGetOnlyElement_noDefault_valid() {
        Iterator<String> iterator = Collections.singletonList("foo").iterator();
        assertEquals("foo", Iterators.getOnlyElement(iterator));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetOnlyElement_noDefault_empty() {
        Iterator<String> iterator = Iterators.emptyIterator();
        Iterators.getOnlyElement(iterator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOnlyElement_withDefault_many() {
        Iterator<String> iterator = newArrayList("foo", "bar").iterator();
        Iterators.getOnlyElement(iterator, "x");
    }

    @Test
    public void testGetOnlyElement_withDefault_singleton() {
        Iterator<String> iterator = Collections.singletonList("foo").iterator();
        assertEquals("foo", Iterators.getOnlyElement(iterator, "bar"));
    }

    @Test
    public void testGetOnlyElement_withDefault_empty() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertEquals("bar", Iterators.getOnlyElement(iterator, "bar"));
    }

    @Test
    public void testGetOnlyElement_withDefault_empty_null() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertNull(Iterators.getOnlyElement(iterator, null));
    }

    @Test
    public void testFilterSimple() {
        Iterator<String> unfiltered = newArrayList("foo", "bar").iterator();
        Iterator<String> filtered = Iterators.filter(unfiltered,
                Predicates.equalTo("foo"));
        List<String> expected = Collections.singletonList("foo");
        List<String> actual = newArrayList(filtered);
        assertEquals(expected, actual);
    }

    @Test
    public void testFilterNoMatch() {
        Iterator<String> unfiltered = newArrayList("foo", "bar").iterator();
        Iterator<String> filtered = Iterators.filter(unfiltered,
                Predicates.alwaysFalse());
        List<String> expected = Collections.emptyList();
        List<String> actual = newArrayList(filtered);
        assertEquals(expected, actual);
    }

    @Test
    public void testFilterMatchAll() {
        Iterator<String> unfiltered = newArrayList("foo", "bar").iterator();
        Iterator<String> filtered = Iterators.filter(unfiltered,
                Predicates.alwaysTrue());
        List<String> expected = newArrayList("foo", "bar");
        List<String> actual = newArrayList(filtered);
        assertEquals(expected, actual);
    }

    @Test
    public void testFilterNothing() {
        Iterator<String> unfiltered = Collections.<String>emptyList().iterator();
        Iterator<String> filtered = Iterators.filter(unfiltered,
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        throw new AssertionError("Should never be evaluated");
                    }
                });

        List<String> expected = Collections.emptyList();
        List<String> actual = newArrayList(filtered);
        assertEquals(expected, actual);
    }

    @Test
    public void testAny() {
        List<String> list = newArrayList();
        Predicate<String> predicate = Predicates.equalTo("pants");

        assertFalse(Iterators.any(list.iterator(), predicate));
        list.add("cool");
        assertFalse(Iterators.any(list.iterator(), predicate));
        list.add("pants");
        assertTrue(Iterators.any(list.iterator(), predicate));
    }

    @Test
    public void testAll() {
        List<String> list = newArrayList();
        Predicate<String> predicate = Predicates.equalTo("cool");

        assertTrue(Iterators.all(list.iterator(), predicate));
        list.add("cool");
        assertTrue(Iterators.all(list.iterator(), predicate));
        list.add("pants");
        assertFalse(Iterators.all(list.iterator(), predicate));
    }

    @Test
    public void testFind_firstElement() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool", Iterators.find(iterator, Predicates.equalTo("cool")));
        assertEquals("pants", iterator.next());
    }

    @Test
    public void testFind_lastElement() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("pants", Iterators.find(iterator,
                Predicates.equalTo("pants")));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFind_notPresent() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        try {
            Iterators.find(iterator, Predicates.alwaysFalse());
            fail();
        } catch (NoSuchElementException e) {
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFind_matchAlways() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool", Iterators.find(iterator, Predicates.alwaysTrue()));
    }

    @Test
    public void testFind_withDefault_first() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool",
                Iterators.find(iterator, Predicates.equalTo("cool"), "woot"));
        assertEquals("pants", iterator.next());
    }

    @Test
    public void testFind_withDefault_last() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("pants",
                Iterators.find(iterator, Predicates.equalTo("pants"), "woot"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFind_withDefault_notPresent() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("woot",
                Iterators.find(iterator, Predicates.alwaysFalse(), "woot"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFind_withDefault_notPresent_nullReturn() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertNull(
                Iterators.find(iterator, Predicates.alwaysFalse(), null));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testFind_withDefault_matchAlways() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool",
                Iterators.find(iterator, Predicates.alwaysTrue(), "woot"));
        assertEquals("pants", iterator.next());
    }

    @Test
    public void testTryFind_firstElement() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool",
                Iterators.tryFind(iterator, Predicates.equalTo("cool")).get());
    }

    @Test
    public void testTryFind_lastElement() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("pants",
                Iterators.tryFind(iterator, Predicates.equalTo("pants")).get());
    }

    @Test
    public void testTryFind_alwaysTrue() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("cool",
                Iterators.tryFind(iterator, Predicates.alwaysTrue()).get());
    }

    @Test
    public void testTryFind_alwaysFalse_orDefault() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertEquals("woot",
                Iterators.tryFind(iterator, Predicates.alwaysFalse()).or("woot"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testTryFind_alwaysFalse_isPresent() {
        Iterable<String> list = newArrayList("cool", "pants");
        Iterator<String> iterator = list.iterator();
        assertFalse(
                Iterators.tryFind(iterator, Predicates.alwaysFalse()).isPresent());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testTransform() {
        Iterator<String> input = asList("1", "2", "3").iterator();
        Iterator<Integer> result = Iterators.transform(input,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String from) {
                        return Integer.valueOf(from);
                    }
                });

        List<Integer> actual = newArrayList(result);
        List<Integer> expected = asList(1, 2, 3);
        assertEquals(expected, actual);
    }

    public void testTransformRemove() {
        List<String> list = newArrayList("1", "2", "3");
        Iterator<String> input = list.iterator();
        Iterator<Integer> iterator = Iterators.transform(input,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String from) {
                        return Integer.valueOf(from);
                    }
                });

        assertEquals(Integer.valueOf(1), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());
        iterator.remove();
        assertEquals(asList("1", "3"), list);
    }

    @Test
    public void testPoorlyBehavedTransform() {
        Iterator<String> input = asList("1", null, "3").iterator();
        Iterator<Integer> result = Iterators.transform(input,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String from) {
                        return Integer.valueOf(from);
                    }
                });

        result.next();
        try {
            result.next();
            fail("Expected NFE");
        } catch (NumberFormatException nfe) {
            // Expected to fail.
        }
    }

    @Test
    public void testNullFriendlyTransform() {
        Iterator<Integer> input = asList(1, 2, null, 3).iterator();
        Iterator<String> result = Iterators.transform(input,
                new Function<Integer, String>() {
                    @Override
                    public String apply(Integer from) {
                        return String.valueOf(from);
                    }
                });

        List<String> actual = newArrayList(result);
        List<String> expected = asList("1", "2", "null", "3");
        assertEquals(expected, actual);
    }

    /**
     * Illustrates the somewhat bizarre behavior when a null is passed in.
     */
    @Test
    public void testConcatContainingNull() {
        @SuppressWarnings("unchecked")
        Iterator<Iterator<Integer>> input
                = asList(iterateOver(1, 2), null, iterateOver(3)).iterator();
        Iterator<Integer> result = Iterators.concat(input);
        assertEquals(1, (int) result.next());
        assertEquals(2, (int) result.next());
        try {
            result.hasNext();
            fail("no exception thrown");
        } catch (NullPointerException e) {
        }
        try {
            result.next();
            fail("no exception thrown");
        } catch (NullPointerException e) {
        }
        // There is no way to get "through" to the 3.  Buh-bye
    }

    @Test
    public void testAddAllWithEmptyIterator() {
        List<String> alreadyThere = newArrayList("already", "there");

        boolean changed = Iterators.addAll(alreadyThere,
                Iterators.<String>emptyIterator());
        assertThat(alreadyThere).containsExactly("already", "there");
        assertFalse(changed);
    }

    @Test
    public void testAddAllToList() {
        List<String> alreadyThere = newArrayList("already", "there");
        List<String> freshlyAdded = newArrayList("freshly", "added");

        boolean changed = Iterators.addAll(alreadyThere, freshlyAdded.iterator());

        assertThat(alreadyThere).containsExactly("already", "there", "freshly", "added");
        assertTrue(changed);
    }

    @Test
    public void testAddAllToSet() {
        Set<String> alreadyThere = new LinkedHashSet<>(asList("already", "there"));
        List<String> oneMore = newArrayList("there");

        boolean changed = Iterators.addAll(alreadyThere, oneMore.iterator());
        assertThat(alreadyThere).containsExactly("already", "there");
        assertFalse(changed);
    }

    private static Iterator<Integer> iterateOver(final Integer... values) {
        return newArrayList(values).iterator();
    }

    @Test
    public void testElementsEqual() {
        Iterable<?> a;
        Iterable<?> b;

        // Base case.
        a = newArrayList();
        b = Collections.emptySet();
        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));

        // A few elements.
        a = asList(4, 8, 15, 16, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));

        // The same, but with nulls.
        a = asList(4, 8, null, 16, 23, 42);
        b = asList(4, 8, null, 16, 23, 42);
        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));

        // Different Iterable types (still equal elements, though).
        a = newArrayList(4, 8, 15, 16, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));

        // An element differs.
        a = asList(4, 8, 15, 12, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterators.elementsEqual(a.iterator(), b.iterator()));

        // null versus non-null.
        a = asList(4, 8, 15, null, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterators.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(Iterators.elementsEqual(b.iterator(), a.iterator()));

        // Different lengths.
        a = asList(4, 8, 15, 16, 23);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterators.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(Iterators.elementsEqual(b.iterator(), a.iterator()));

        // Different lengths, one is empty.
        a = Collections.emptySet();
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterators.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(Iterators.elementsEqual(b.iterator(), a.iterator()));
    }

    @Test
    public void testPartition_badSize() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        try {
            Iterators.partition(source, 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testPartition_empty() {
        Iterator<Integer> source = Iterators.emptyIterator();
        Iterator<List<Integer>> partitions = Iterators.partition(source, 1);
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPartition_singleton1() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        Iterator<List<Integer>> partitions = Iterators.partition(source, 1);
        assertTrue(partitions.hasNext());
        assertTrue(partitions.hasNext());
        assertThat(partitions.next()).containsOnly(1);
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPartition_singleton2() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        Iterator<List<Integer>> partitions = Iterators.partition(source, 2);
        assertTrue(partitions.hasNext());
        assertTrue(partitions.hasNext());
        assertThat(partitions.next()).containsOnly(1);
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPartition_view() {
        List<Integer> list = asList(1, 2);
        Iterator<List<Integer>> partitions
                = Iterators.partition(list.iterator(), 1);

        // Changes before the partition is retrieved are reflected
        list.set(0, 3);
        List<Integer> first = partitions.next();

        // Changes after are not
        list.set(0, 4);

        assertThat(first).containsOnly(3);
    }

    @Test
    public void testPartitionRandomAccess() {
        Iterator<Integer> source = asList(1, 2, 3).iterator();
        Iterator<List<Integer>> partitions = Iterators.partition(source, 2);
        assertTrue(partitions.next() instanceof RandomAccess);
        assertTrue(partitions.next() instanceof RandomAccess);
    }

    @Test
    public void testPaddedPartition_badSize() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        try {
            Iterators.paddedPartition(source, 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testPaddedPartition_empty() {
        Iterator<Integer> source = Iterators.emptyIterator();
        Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 1);
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPaddedPartition_singleton1() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 1);
        assertTrue(partitions.hasNext());
        assertTrue(partitions.hasNext());
        assertThat(partitions.next()).containsOnly(1);
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPaddedPartition_singleton2() {
        Iterator<Integer> source = Collections.singleton(1).iterator();
        Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 2);
        assertTrue(partitions.hasNext());
        assertTrue(partitions.hasNext());
        assertEquals(asList(1, null), partitions.next());
        assertFalse(partitions.hasNext());
    }

    @Test
    public void testPaddedPartition_view() {
        List<Integer> list = asList(1, 2);
        Iterator<List<Integer>> partitions
                = Iterators.paddedPartition(list.iterator(), 1);

        // Changes before the PaddedPartition is retrieved are reflected
        list.set(0, 3);
        List<Integer> first = partitions.next();

        // Changes after are not
        list.set(0, 4);

        assertThat(first).containsOnly(3);
    }

    @Test
    public void testPaddedPartitionRandomAccess() {
        Iterator<Integer> source = asList(1, 2, 3).iterator();
        Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 2);
        assertTrue(partitions.next() instanceof RandomAccess);
        assertTrue(partitions.next() instanceof RandomAccess);
    }

    @Test
    public void testToString() {
        Iterator<String> iterator = newArrayList("yam", "bam", "jam", "ham").iterator();
        assertEquals("[yam, bam, jam, ham]", Iterators.toString(iterator));
    }

    @Test
    public void testToStringWithNull() {
        Iterator<String> iterator = newArrayList("hello", null, "world").iterator();
        assertEquals("[hello, null, world]", Iterators.toString(iterator));
    }

    @Test
    public void testToStringEmptyIterator() {
        Iterator<String> iterator = Collections.<String>emptyList().iterator();
        assertEquals("[]", Iterators.toString(iterator));
    }

    @Test
    public void testGetNext_withDefault_singleton() {
        Iterator<String> iterator = Collections.singletonList("foo").iterator();
        assertEquals("foo", Iterators.getNext(iterator, "bar"));
    }

    @Test
    public void testGetNext_withDefault_empty() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertEquals("bar", Iterators.getNext(iterator, "bar"));
    }

    @Test
    public void testGetNext_withDefault_empty_null() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertNull(Iterators.getNext(iterator, null));
    }

    @Test
    public void testGetNext_withDefault_two() {
        Iterator<String> iterator = asList("foo", "bar").iterator();
        assertEquals("foo", Iterators.getNext(iterator, "x"));
    }

    @Test
    public void testGetLast_basic() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        assertEquals("b", getLast(list.iterator()));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetLast_exception() {
        getLast(Collections.emptyList().iterator());
    }

    @Test
    public void testGetLast_withDefault_singleton() {
        Iterator<String> iterator = Collections.singletonList("foo").iterator();
        assertEquals("foo", getLast(iterator, "bar"));
    }

    @Test
    public void testGetLast_withDefault_empty() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertEquals("bar", getLast(iterator, "bar"));
    }

    @Test
    public void testGetLast_withDefault_empty_null() {
        Iterator<String> iterator = Iterators.emptyIterator();
        assertNull(getLast(iterator, null));
    }

    @Test
    public void testGetLast_withDefault_two() {
        Iterator<String> iterator = asList("foo", "bar").iterator();
        assertEquals("bar", getLast(iterator, "x"));
    }

    @Test
    public void testGet_basic() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        assertEquals("b", get(iterator, 1));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_atSize() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        try {
            get(iterator, 2);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_pastEnd() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        try {
            get(iterator, 5);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_empty() {
        List<String> list = new ArrayList<>();
        Iterator<String> iterator = list.iterator();
        try {
            get(iterator, 0);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_negativeIndex() {
        List<String> list = newArrayList("a", "b", "c");
        Iterator<String> iterator = list.iterator();
        try {
            get(iterator, -1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
    }

    @Test
    public void testGet_withDefault_basic() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        assertEquals("a", get(iterator, 0, "c"));
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testGet_withDefault_atSize() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        assertEquals("c", get(iterator, 2, "c"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_withDefault_pastEnd() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        assertEquals("c", get(iterator, 3, "c"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet_withDefault_negativeIndex() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        try {
            get(iterator, -1, "c");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            // pass
        }
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testAdvance_basic() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        advance(iterator, 1);
        assertEquals("b", iterator.next());
    }

    @Test
    public void testAdvance_pastEnd() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Iterator<String> iterator = list.iterator();
        advance(iterator, 5);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAdvance_illegalArgument() {
        List<String> list = newArrayList("a", "b", "c");
        Iterator<String> iterator = list.iterator();
        try {
            advance(iterator, -1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testRemoveAll() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterators.removeAll(
                list.iterator(), newArrayList("b", "d", "f")));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterators.removeAll(
                list.iterator(), newArrayList("x", "y", "z")));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    @Test
    public void testRemoveIf() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterators.removeIf(
                list.iterator(),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("b") || s.equals("d") || s.equals("f");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterators.removeIf(
                list.iterator(),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("x") || s.equals("y") || s.equals("z");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    @Test
    public void testRetainAll() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterators.retainAll(
                list.iterator(), newArrayList("b", "d", "f")));
        assertEquals(newArrayList("b", "d"), list);
        assertFalse(Iterators.retainAll(
                list.iterator(), newArrayList("b", "e", "d")));
        assertEquals(newArrayList("b", "d"), list);
    }

    @Test
    public void testIndexOf_consumedData() {
        Iterator<String> iterator =
                newArrayList("manny", "mo", "jack").iterator();
        assertEquals(1, Iterators.indexOf(iterator, Predicates.equalTo("mo")));
        assertEquals("jack", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIndexOf_consumedDataWithDuplicates() {
        Iterator<String> iterator =
                newArrayList("manny", "mo", "mo", "jack").iterator();
        assertEquals(1, Iterators.indexOf(iterator, Predicates.equalTo("mo")));
        assertEquals("mo", iterator.next());
        assertEquals("jack", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIndexOf_consumedDataNoMatch() {
        Iterator<String> iterator =
                newArrayList("manny", "mo", "mo", "jack").iterator();
        assertEquals(-1, Iterators.indexOf(iterator, Predicates.equalTo("bob")));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testPeekingIteratorShortCircuit() {
        Iterator<String> nonpeek = newArrayList("a", "b", "c").iterator();
        PeekingIterator<String> peek = Iterators.peekingIterator(nonpeek);
        assertNotSame(peek, nonpeek);
        assertSame(peek, Iterators.peekingIterator(peek));
        assertSame(peek, Iterators.peekingIterator(peek));
    }
}
