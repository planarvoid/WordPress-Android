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

import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import com.soundcloud.java.optional.Optional;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Unit test for {@code Iterables}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
public class IterablesTest {

    @Test
    public void testSize0() {
        Iterable<String> iterable = Collections.emptySet();
        assertEquals(0, Iterables.size(iterable));
    }

    @Test
    public void testSize1Collection() {
        Iterable<String> iterable = Collections.singleton("a");
        assertEquals(1, Iterables.size(iterable));
    }

    @Test
    public void testSize2NonCollection() {
        Iterable<Integer> iterable = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return asList(0, 1).iterator();
            }
        };
        assertEquals(2, Iterables.size(iterable));
    }

    @SuppressWarnings("serial")
    @Test
    public void testSize_collection_doesntIterate() {
        List<Integer> nums = asList(1, 2, 3, 4, 5);
        List<Integer> collection = new ArrayList<Integer>(nums) {
            @Override
            public Iterator<Integer> iterator() {
                throw new AssertionError("Don't iterate me!");
            }
        };
        assertEquals(5, Iterables.size(collection));
    }

    private static Iterable<String> iterable(String... elements) {
        final List<String> list = asList(elements);
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return list.iterator();
            }
        };
    }

    @Test
    public void test_contains_null_set_yes() {
        Iterable<String> set = Sets.newHashSet("a", null, "b");
        assertTrue(Iterables.contains(set, null));
    }

    @Test
    public void test_contains_null_set_no() {
        Iterable<String> set = Sets.newHashSet("a", "b");
        assertFalse(Iterables.contains(set, null));
    }

    @Test
    public void test_contains_null_iterable_yes() {
        Iterable<String> set = iterable("a", null, "b");
        assertTrue(Iterables.contains(set, null));
    }

    @Test
    public void test_contains_null_iterable_no() {
        Iterable<String> set = iterable("a", "b");
        assertFalse(Iterables.contains(set, null));
    }

    @Test
    public void test_contains_nonnull_set_yes() {
        Iterable<String> set = Sets.newHashSet("a", null, "b");
        assertTrue(Iterables.contains(set, "b"));
    }

    @Test
    public void test_contains_nonnull_set_no() {
        Iterable<String> set = Sets.newHashSet("a", "b");
        assertFalse(Iterables.contains(set, "c"));
    }

    @Test
    public void test_contains_nonnull_iterable_yes() {
        Iterable<String> set = iterable("a", null, "b");
        assertTrue(Iterables.contains(set, "b"));
    }

    @Test
    public void test_contains_nonnull_iterable_no() {
        Iterable<String> set = iterable("a", "b");
        assertFalse(Iterables.contains(set, "c"));
    }

    @Test
    public void testGetOnlyElement_noDefault_valid() {
        Iterable<String> iterable = singletonList("foo");
        assertEquals("foo", Iterables.getOnlyElement(iterable));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetOnlyElement_noDefault_empty() {
        Iterable<String> iterable = emptyList();
        Iterables.getOnlyElement(iterable);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOnlyElement_noDefault_multiple() {
        Iterable<String> iterable = asList("foo", "bar");
        Iterables.getOnlyElement(iterable);
    }

    @Test
    public void testGetOnlyElement_withDefault_singleton() {
        Iterable<String> iterable = singletonList("foo");
        assertEquals("foo", Iterables.getOnlyElement(iterable, "bar"));
    }

    @Test
    public void testGetOnlyElement_withDefault_empty() {
        Iterable<String> iterable = emptyList();
        assertEquals("bar", Iterables.getOnlyElement(iterable, "bar"));
    }

    @Test
    public void testGetOnlyElement_withDefault_empty_null() {
        Iterable<String> iterable = emptyList();
        assertNull(Iterables.getOnlyElement(iterable, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOnlyElement_withDefault_multiple() {
        Iterable<String> iterable = asList("foo", "bar");
        Iterables.getOnlyElement(iterable, "x");
    }

    @Test
    public void testToArrayEmpty() {
        Iterable<String> iterable = emptyList();
        String[] array = Iterables.toArray(iterable, String.class);
        assertTrue(Arrays.equals(new String[0], array));
    }

    @Test
    public void testToArraySingleton() {
        Iterable<String> iterable = singletonList("a");
        String[] array = Iterables.toArray(iterable, String.class);
        assertTrue(Arrays.equals(new String[]{"a"}, array));
    }

    @Test
    public void testToArray() {
        String[] sourceArray = new String[]{"a", "b", "c"};
        Iterable<String> iterable = asList(sourceArray);
        String[] newArray = Iterables.toArray(iterable, String.class);
        assertTrue(Arrays.equals(sourceArray, newArray));
    }

    @Test
    public void testAny() {
        List<String> list = newArrayList();
        Predicate<String> predicate = Predicates.equalTo("pants");

        assertFalse(Iterables.any(list, predicate));
        list.add("cool");
        assertFalse(Iterables.any(list, predicate));
        list.add("pants");
        assertTrue(Iterables.any(list, predicate));
    }

    @Test
    public void testAll() {
        List<String> list = newArrayList();
        Predicate<String> predicate = Predicates.equalTo("cool");

        assertTrue(Iterables.all(list, predicate));
        list.add("cool");
        assertTrue(Iterables.all(list, predicate));
        list.add("pants");
        assertFalse(Iterables.all(list, predicate));
    }

    @Test
    public void testFind() {
        Iterable<String> list = newArrayList("cool", "pants");
        assertEquals("cool", Iterables.find(list, Predicates.equalTo("cool")));
        assertEquals("pants", Iterables.find(list, Predicates.equalTo("pants")));
        try {
            Iterables.find(list, Predicates.alwaysFalse());
            fail();
        } catch (NoSuchElementException e) {
        }
        assertEquals("cool", Iterables.find(list, Predicates.alwaysTrue()));
        assertCanIterateAgain(list);
    }

    @Test
    public void testFind_withDefault() {
        Iterable<String> list = newArrayList("cool", "pants");
        assertEquals("cool",
                Iterables.find(list, Predicates.equalTo("cool"), "woot"));
        assertEquals("pants",
                Iterables.find(list, Predicates.equalTo("pants"), "woot"));
        assertEquals("woot", Iterables.find(list,
                Predicates.alwaysFalse(), "woot"));
        assertNull(Iterables.find(list, Predicates.alwaysFalse(), null));
        assertEquals("cool",
                Iterables.find(list, Predicates.alwaysTrue(), "woot"));
        assertCanIterateAgain(list);
    }

    @Test
    public void testTryFind() {
        Iterable<String> list = newArrayList("cool", "pants");
        assertEquals(Optional.of("cool"),
                Iterables.tryFind(list, Predicates.equalTo("cool")));
        assertEquals(Optional.of("pants"),
                Iterables.tryFind(list, Predicates.equalTo("pants")));
        assertEquals(Optional.of("cool"),
                Iterables.tryFind(list, Predicates.alwaysTrue()));
        assertEquals(Optional.absent(),
                Iterables.tryFind(list, Predicates.alwaysFalse()));
        assertCanIterateAgain(list);
    }

    private static class TypeA {
    }

    private interface TypeB {
    }

    private static class HasBoth extends TypeA implements TypeB {
    }

    @Test
    public void testFilterByType() throws Exception {
        HasBoth hasBoth = new HasBoth();
        Iterable<TypeA> alist =
                newArrayList(new TypeA(), new TypeA(), hasBoth, new TypeA());
        Iterable<TypeB> blist = Iterables.filter(alist, TypeB.class);
        assertThat(blist).containsExactly(hasBoth);
    }

    @Test
    public void testTransform() {
        List<String> input = asList("1", "2", "3");
        Iterable<Integer> result = Iterables.transform(input,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String from) {
                        return Integer.valueOf(from);
                    }
                });

        List<Integer> actual = newArrayList(result);
        List<Integer> expected = asList(1, 2, 3);
        assertEquals(expected, actual);
        assertCanIterateAgain(result);
        assertEquals("[1, 2, 3]", Iterables.toString(result));
    }

    @Test
    public void testPoorlyBehavedTransform() {
        List<String> input = asList("1", null, "3");
        Iterable<Integer> result = Iterables.transform(input,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String from) {
                        return Integer.valueOf(from);
                    }
                });

        Iterator<Integer> resultIterator = result.iterator();
        resultIterator.next();

        try {
            resultIterator.next();
            fail("Expected NFE");
        } catch (NumberFormatException nfe) {
            // Expected to fail.
        }
    }

    @Test
    public void testNullFriendlyTransform() {
        List<Integer> input = asList(1, 2, null, 3);
        Iterable<String> result = Iterables.transform(input,
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

    // Again, the exhaustive tests are in IteratorsTest
    @Test
    public void testConcatIterable() {
        List<Integer> list1 = newArrayList(1);
        List<Integer> list2 = newArrayList(4);

        @SuppressWarnings("unchecked")
        List<List<Integer>> input = newArrayList(list1, list2);

        Iterable<Integer> result = Iterables.concat(input);
        assertEquals(asList(1, 4), newArrayList(result));

        // Now change the inputs and see result dynamically change as well

        list1.add(2);
        List<Integer> list3 = newArrayList(3);
        input.add(1, list3);

        assertEquals(asList(1, 2, 3, 4), newArrayList(result));
        assertEquals("[1, 2, 3, 4]", Iterables.toString(result));
    }

    @Test
    public void testConcatVarargs() {
        List<Integer> list1 = newArrayList(1);
        List<Integer> list2 = newArrayList(4);
        List<Integer> list3 = newArrayList(7, 8);
        List<Integer> list4 = newArrayList(9);
        List<Integer> list5 = newArrayList(10);
        @SuppressWarnings("unchecked")
        Iterable<Integer> result =
                Iterables.concat(list1, list2, list3, list4, list5);
        assertEquals(asList(1, 4, 7, 8, 9, 10), newArrayList(result));
        assertEquals("[1, 4, 7, 8, 9, 10]", Iterables.toString(result));
    }

    @Ignore // we can't do this, since this was actually done by ImmutableSet, not concat()
    @Test(expected = NullPointerException.class)
    public void testConcatNullPointerException() {
        List<Integer> list1 = newArrayList(1);
        List<Integer> list2 = newArrayList(4);

        Iterables.concat(list1, null, list2);
    }

    @Test
    public void testConcatPeformingFiniteCycle() {
        Iterable<Integer> iterable = asList(1, 2, 3);
        int n = 4;
        Iterable<Integer> repeated
                = Iterables.concat(Collections.nCopies(n, iterable));
        assertThat(repeated).containsExactly(
                1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPartition_badSize() {
        Iterable<Integer> source = Collections.singleton(1);
        Iterables.partition(source, 0);
    }

    @Test
    public void testPartition_empty() {
        Iterable<Integer> source = Collections.emptySet();
        Iterable<List<Integer>> partitions = Iterables.partition(source, 1);
        assertTrue(Iterables.isEmpty(partitions));
    }

    @Test
    public void testPartition_singleton1() {
        Iterable<Integer> source = Collections.singleton(1);
        Iterable<List<Integer>> partitions = Iterables.partition(source, 1);
        assertEquals(1, Iterables.size(partitions));
        assertEquals(singletonList(1), partitions.iterator().next());
    }

    @Test
    public void testPartition_view() {
        List<Integer> list = asList(1, 2);
        Iterable<List<Integer>> partitions = Iterables.partition(list, 2);

        // Changes before the partition is retrieved are reflected
        list.set(0, 3);

        Iterator<List<Integer>> iterator = partitions.iterator();

        // Changes before the partition is retrieved are reflected
        list.set(1, 4);

        List<Integer> first = iterator.next();

        // Changes after are not
        list.set(0, 5);

        assertEquals(newArrayList(3, 4), first);
    }

    @Test
    public void testPartitionRandomAccessInput() {
        Iterable<Integer> source = asList(1, 2, 3);
        Iterable<List<Integer>> partitions = Iterables.partition(source, 2);
        Iterator<List<Integer>> iterator = partitions.iterator();
        assertTrue(iterator.next() instanceof RandomAccess);
        assertTrue(iterator.next() instanceof RandomAccess);
    }

    @Test
    public void testPartitionNonRandomAccessInput() {
        Iterable<Integer> source = new LinkedList<>(asList(1, 2, 3));
        Iterable<List<Integer>> partitions = Iterables.partition(source, 2);
        Iterator<List<Integer>> iterator = partitions.iterator();
        // Even though the input list doesn't implement RandomAccess, the output
        // lists do.
        assertTrue(iterator.next() instanceof RandomAccess);
        assertTrue(iterator.next() instanceof RandomAccess);
    }

    @Test
    public void testPaddedPartition_basic() {
        List<Integer> list = asList(1, 2, 3, 4, 5);
        Iterable<List<Integer>> partitions = Iterables.paddedPartition(list, 2);
        assertEquals(3, Iterables.size(partitions));
        assertEquals(asList(5, null), Iterables.getLast(partitions));
    }

    @Test
    public void testPaddedPartitionRandomAccessInput() {
        Iterable<Integer> source = asList(1, 2, 3);
        Iterable<List<Integer>> partitions = Iterables.paddedPartition(source, 2);
        Iterator<List<Integer>> iterator = partitions.iterator();
        assertTrue(iterator.next() instanceof RandomAccess);
        assertTrue(iterator.next() instanceof RandomAccess);
    }

    @Test
    public void testPaddedPartitionNonRandomAccessInput() {
        Iterable<Integer> source = new LinkedList<>(asList(1, 2, 3));
        Iterable<List<Integer>> partitions = Iterables.paddedPartition(source, 2);
        Iterator<List<Integer>> iterator = partitions.iterator();
        // Even though the input list doesn't implement RandomAccess, the output
        // lists do.
        assertTrue(iterator.next() instanceof RandomAccess);
        assertTrue(iterator.next() instanceof RandomAccess);
    }

    @Test
    // More tests in IteratorsTest
    public void testAddAllToList() {
        List<String> alreadyThere = newArrayList("already", "there");
        List<String> freshlyAdded = newArrayList("freshly", "added");

        boolean changed = Iterables.addAll(alreadyThere, freshlyAdded);
        assertThat(alreadyThere).containsExactly("already", "there", "freshly", "added");
        assertTrue(changed);
    }

    private static void assertCanIterateAgain(Iterable<?> iterable) {
        for (@SuppressWarnings("unused") Object obj : iterable) {
            // nop
        }
    }

    @Test
    // More exhaustive tests are in IteratorsTest.
    public void testElementsEqual() throws Exception {
        Iterable<?> a;
        Iterable<?> b;

        // A few elements.
        a = asList(4, 8, 15, 16, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertTrue(Iterables.elementsEqual(a, b));

        // An element differs.
        a = asList(4, 8, 15, 12, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterables.elementsEqual(a, b));

        // null versus non-null.
        a = asList(4, 8, 15, null, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterables.elementsEqual(a, b));
        assertFalse(Iterables.elementsEqual(b, a));

        // Different lengths.
        a = asList(4, 8, 15, 16, 23);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(Iterables.elementsEqual(a, b));
        assertFalse(Iterables.elementsEqual(b, a));
    }

    @Test
    public void testToString() {
        List<String> list = emptyList();
        assertEquals("[]", Iterables.toString(list));

        list = newArrayList("yam", "bam", "jam", "ham");
        assertEquals("[yam, bam, jam, ham]", Iterables.toString(list));
    }

    @Test
    public void testIsEmpty() {
        Iterable<String> emptyList = emptyList();
        assertTrue(Iterables.isEmpty(emptyList));

        Iterable<String> singletonList = singletonList("foo");
        assertFalse(Iterables.isEmpty(singletonList));
    }

    private void testGetOnAbc(Iterable<String> iterable) {
        try {
            Iterables.get(iterable, -1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        assertEquals("a", Iterables.get(iterable, 0));
        assertEquals("b", Iterables.get(iterable, 1));
        assertEquals("c", Iterables.get(iterable, 2));
        try {
            Iterables.get(iterable, 3);
            fail();
        } catch (IndexOutOfBoundsException nsee) {
        }
        try {
            Iterables.get(iterable, 4);
            fail();
        } catch (IndexOutOfBoundsException nsee) {
        }
    }

    private void testGetOnEmpty(Iterable<String> iterable) {
        try {
            Iterables.get(iterable, 0);
            fail();
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void testGet_list() {
        testGetOnAbc(newArrayList("a", "b", "c"));
    }

    @Test
    public void testGet_emptyList() {
        testGetOnEmpty(Collections.<String>emptyList());
    }

    @Test
    public void testGet_sortedSet() {
        testGetOnAbc(new TreeSet<String>(asList("b", "c", "a")));
    }

    @Test
    public void testGet_emptySortedSet() {
        testGetOnEmpty(Collections.<String>emptySet());
    }

    @Test
    public void testGet_iterable() {
        testGetOnAbc(asList("a", "b", "c"));
    }

    @Test
    public void testGet_emptyIterable() {
        testGetOnEmpty(Sets.<String>newHashSet());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGet_withDefault_negativePosition() {
        Iterables.get(newArrayList("a", "b", "c"), -1, "d");
    }

    @Test
    public void testGet_withDefault_simple() {
        ArrayList<String> list = newArrayList("a", "b", "c");
        assertEquals("b", Iterables.get(list, 1, "d"));
    }

    @Test
    public void testGet_withDefault_iterable() {
        Set<String> set = Sets.newHashSet("a", "b", "c");
        assertEquals("b", Iterables.get(set, 1, "d"));
    }

    @Test
    public void testGet_withDefault_last() {
        ArrayList<String> list = newArrayList("a", "b", "c");
        assertEquals("c", Iterables.get(list, 2, "d"));
    }

    @Test
    public void testGet_withDefault_lastPlusOne() {
        ArrayList<String> list = newArrayList("a", "b", "c");
        assertEquals("d", Iterables.get(list, 3, "d"));
    }

    @Test
    public void testGet_withDefault_doesntIterate() {
        List<String> list = new DiesOnIteratorArrayList();
        list.add("a");
        assertEquals("a", Iterables.get(list, 0, "b"));
    }

    @Test
    public void testGetFirst_withDefault_singleton() {
        Iterable<String> iterable = singletonList("foo");
        assertEquals("foo", Iterables.getFirst(iterable, "bar"));
    }

    @Test
    public void testGetFirst_withDefault_empty() {
        Iterable<String> iterable = emptyList();
        assertEquals("bar", Iterables.getFirst(iterable, "bar"));
    }

    @Test
    public void testGetFirst_withDefault_empty_null() {
        Iterable<String> iterable = emptyList();
        assertNull(Iterables.getFirst(iterable, null));
    }

    @Test
    public void testGetFirst_withDefault_multiple() {
        Iterable<String> iterable = asList("foo", "bar");
        assertEquals("foo", Iterables.getFirst(iterable, "qux"));
    }

    @Test
    public void testGetLast_list() {
        List<String> list = newArrayList("a", "b", "c");
        assertEquals("c", Iterables.getLast(list));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetLast_emptyList() {
        List<String> list = emptyList();
        Iterables.getLast(list);
    }

    @Test
    public void testGetLast_sortedSet() {
        SortedSet<String> sortedSet = new TreeSet<>(asList("b", "c", "a"));
        assertEquals("c", Iterables.getLast(sortedSet));
    }

    @Test
    public void testGetLast_withDefault_singleton() {
        Iterable<String> iterable = singletonList("foo");
        assertEquals("foo", Iterables.getLast(iterable, "bar"));
    }

    @Test
    public void testGetLast_withDefault_empty() {
        Iterable<String> iterable = emptyList();
        assertEquals("bar", Iterables.getLast(iterable, "bar"));
    }

    @Test
    public void testGetLast_withDefault_empty_null() {
        Iterable<String> iterable = emptyList();
        assertNull(Iterables.getLast(iterable, null));
    }

    @Test
    public void testGetLast_withDefault_multiple() {
        Iterable<String> iterable = asList("foo", "bar");
        assertEquals("bar", Iterables.getLast(iterable, "qux"));
    }

    /**
     * {@link ArrayList} extension that forbids the use of
     * {@link Collection#iterator} for tests that need to prove that it isn't
     * called.
     */
    private static class DiesOnIteratorArrayList extends ArrayList<String> {
        /**
         * @throws UnsupportedOperationException all the time
         */
        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testGetLast_withDefault_not_empty_list() {
        // TODO: verify that this is the best testing strategy.
        List<String> diesOnIteratorList = new DiesOnIteratorArrayList();
        diesOnIteratorList.add("bar");

        assertEquals("bar", Iterables.getLast(diesOnIteratorList, "qux"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetLast_emptySortedSet() {
        SortedSet<String> sortedSet = new TreeSet<>();
        Iterables.getLast(sortedSet);
    }

    @Test
    public void testGetLast_iterable() {
        Set<String> set = Sets.newHashSet("a", "b", "c");
        assertEquals("c", Iterables.getLast(set));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetLast_emptyIterable() {
        Set<String> set = Sets.newHashSet();
        Iterables.getLast(set);
    }

    @Test
    public void testRemoveAll_collection() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterables.removeAll(list, newArrayList("b", "d", "f")));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterables.removeAll(list, newArrayList("x", "y", "z")));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    @Test
    public void testRemoveAll_iterable() {
        final List<String> list = newArrayList("a", "b", "c", "d", "e");
        Iterable<String> iterable = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return list.iterator();
            }
        };
        assertTrue(Iterables.removeAll(iterable, newArrayList("b", "d", "f")));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterables.removeAll(iterable, newArrayList("x", "y", "z")));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    @Test
    public void testRetainAll_collection() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterables.retainAll(list, newArrayList("b", "d", "f")));
        assertEquals(newArrayList("b", "d"), list);
        assertFalse(Iterables.retainAll(list, newArrayList("b", "e", "d")));
        assertEquals(newArrayList("b", "d"), list);
    }

    @Test
    public void testRetainAll_iterable() {
        final List<String> list = newArrayList("a", "b", "c", "d", "e");
        Iterable<String> iterable = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return list.iterator();
            }
        };
        assertTrue(Iterables.retainAll(iterable, newArrayList("b", "d", "f")));
        assertEquals(newArrayList("b", "d"), list);
        assertFalse(Iterables.retainAll(iterable, newArrayList("b", "e", "d")));
        assertEquals(newArrayList("b", "d"), list);
    }

    @Test
    public void testRemoveIf_randomAccess() {
        List<String> list = newArrayList("a", "b", "c", "d", "e");
        assertTrue(Iterables.removeIf(list,
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("b") || s.equals("d") || s.equals("f");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterables.removeIf(list,
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("x") || s.equals("y") || s.equals("z");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    @Test
    public void testRemoveIf_transformedList() {
        List<String> list = newArrayList("1", "2", "3", "4", "5");
        List<Integer> transformed = Lists.transform(list,
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(String s) {
                        return Integer.valueOf(s);
                    }
                });
        assertTrue(Iterables.removeIf(transformed,
                new Predicate<Integer>() {
                    @Override
                    public boolean apply(Integer n) {
                        return (n & 1) == 0;  // isEven()
                    }
                }));
        assertEquals(newArrayList("1", "3", "5"), list);
        assertFalse(Iterables.removeIf(transformed,
                new Predicate<Integer>() {
                    @Override
                    public boolean apply(Integer n) {
                        return (n & 1) == 0;  // isEven()
                    }
                }));
        assertEquals(newArrayList("1", "3", "5"), list);
    }

    @Test
    public void testRemoveIf_noRandomAccess() {
        List<String> list = new LinkedList<>(asList("a", "b", "c", "d", "e"));
        assertTrue(Iterables.removeIf(list,
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("b") || s.equals("d") || s.equals("f");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
        assertFalse(Iterables.removeIf(list,
                new Predicate<String>() {
                    @Override
                    public boolean apply(String s) {
                        return s.equals("x") || s.equals("y") || s.equals("z");
                    }
                }));
        assertEquals(newArrayList("a", "c", "e"), list);
    }

    // The Maps returned by Maps.filterEntries(), Maps.filterKeys(), and
    // Maps.filterValues() are not tested with removeIf() since Maps are not
    // Iterable.  Those returned by Iterators.filter() and Iterables.filter()
    // are not tested because they are unmodifiable.

    @Test
    public void testIterableWithToString() {
        assertEquals("[]", Iterables.toString(emptyList()));
        assertEquals("[a]", Iterables.toString(singletonList("a")));
        assertEquals("[a, b, c]", Iterables.toString(asList("a", "b", "c")));
        assertEquals("[c, a, a]", Iterables.toString(asList("c", "a", "a")));
    }

    @Test
    public void testIterableWithToStringNull() {
        assertEquals("[null]", Iterables.toString(singletonList(null)));
        assertEquals("[null, null]", Iterables.toString(asList(null, null)));
        assertEquals("[, null, a]", Iterables.toString(asList("", null, "a")));
    }

    @Test
    public void testIndexOf_empty() {
        List<String> list = new ArrayList<>();
        assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("")));
    }

    @Test
    public void testIndexOf_oneElement() {
        List<String> list = newArrayList("bob");
        assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("bob")));
        assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
    }

    @Test
    public void testIndexOf_twoElements() {
        List<String> list = newArrayList("mary", "bob");
        assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("mary")));
        assertEquals(1, Iterables.indexOf(list, Predicates.equalTo("bob")));
        assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
    }

    @Test
    public void testIndexOf_withDuplicates() {
        List<String> list =
                newArrayList("mary", "bob", "bob", "bob", "sam");
        assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("mary")));
        assertEquals(1, Iterables.indexOf(list, Predicates.equalTo("bob")));
        assertEquals(4, Iterables.indexOf(list, Predicates.equalTo("sam")));
        assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
    }

    private static final Predicate<CharSequence> STARTSWITH_A =
            new Predicate<CharSequence>() {
                @Override
                public boolean apply(CharSequence input) {
                    return (input.length() > 0) && (input.charAt(0) == 'a');
                }
            };

    @Test
    public void testIndexOf_genericPredicate() {
        List<CharSequence> sequences = newArrayList();
        sequences.add("bob");
        sequences.add(new StringBuilder("charlie"));
        sequences.add(new StringBuffer("henry"));
        sequences.add(new StringBuilder("apple"));
        sequences.add("lemon");

        assertEquals(3, Iterables.indexOf(sequences, STARTSWITH_A));
    }

    @Test
    public void testIndexOf_genericPredicate2() {
        List<String> sequences =
                newArrayList("bob", "charlie", "henry", "apple", "lemon");
        assertEquals(3, Iterables.indexOf(sequences, STARTSWITH_A));
    }
}
