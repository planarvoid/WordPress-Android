/*
 * Copyright (C) 2012 The Guava Authors
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import com.soundcloud.java.test.EqualsTester;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * Tests for filtered collection views.
 *
 * @author Louis Wasserman
 */
@SuppressWarnings("unused")
public class FilteredCollectionsTest {
    private static final Predicate<Integer> EVEN = new Predicate<Integer>() {
        @Override
        public boolean apply(Integer input) {
            return input % 2 == 0;
        }
    };

    private static final Predicate<Integer> PRIME_DIGIT =
            Predicates.in(Sets.newHashSet(2, 3, 5, 7));

    private static final List<? extends List<Integer>> SAMPLE_INPUTS =
            Lists.<List<Integer>>newArrayList(Lists.<Integer>newArrayList(),
                    Lists.newArrayList(1),
                    Lists.newArrayList(2),
                    Lists.newArrayList(2, 3),
                    Lists.newArrayList(1, 2),
                    Lists.newArrayList(3, 5),
                    Lists.newArrayList(2, 4),
                    Lists.newArrayList(1, 2, 3, 5, 6, 8, 9));

  /*
   * We have a whole series of abstract test classes that "stack", so e.g. the tests for filtered
   * NavigableSets inherit the tests for filtered Iterables, Collections, Sets, and SortedSets. The
   * actual implementation tests are further down.
   */

    public static abstract class AbstractFilteredIterableTest<C extends Iterable<Integer>> {
        abstract C createUnfiltered(Iterable<Integer> contents);

        abstract C filter(C elements, Predicate<? super Integer> predicate);

        @Test
        public void testIterationOrderPreserved() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C unfiltered = createUnfiltered(contents);
                C filtered = filter(unfiltered, EVEN);

                Iterator<Integer> filteredItr = filtered.iterator();
                for (Integer i : unfiltered) {
                    if (EVEN.apply(i)) {
                        assertTrue(filteredItr.hasNext());
                        assertEquals(i, filteredItr.next());
                    }
                }
                assertFalse(filteredItr.hasNext());
            }
        }
    }

    public static abstract class AbstractFilteredCollectionTest<C extends Collection<Integer>>
            extends AbstractFilteredIterableTest<C> {

        @Test
        public void testReadsThroughAdd() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C unfiltered = createUnfiltered(contents);
                C filterThenAdd = filter(unfiltered, EVEN);
                unfiltered.add(4);

                List<Integer> target = Lists.newArrayList(contents);
                target.add(4);
                C addThenFilter = filter(createUnfiltered(target), EVEN);

                assertThat(filterThenAdd).containsExactlyElementsOf(addThenFilter);
            }
        }

        @Test
        public void testAdd() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int toAdd = 0; toAdd < 10; toAdd++) {
                    boolean expectedResult = createUnfiltered(contents).add(toAdd);

                    C filtered = filter(createUnfiltered(contents), EVEN);
                    try {
                        assertEquals(expectedResult, filtered.add(toAdd));
                        assertTrue(EVEN.apply(toAdd));
                    } catch (IllegalArgumentException e) {
                        assertFalse(EVEN.apply(toAdd));
                    }
                }
            }
        }

        @Test
        public void testRemove() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int toRemove = 0; toRemove < 10; toRemove++) {
                    assertEquals(contents.contains(toRemove) && EVEN.apply(toRemove),
                            filter(createUnfiltered(contents), EVEN).remove(toRemove));
                }
            }
        }

        @Test
        public void testContains() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    assertEquals(EVEN.apply(i) && contents.contains(i),
                            filter(createUnfiltered(contents), EVEN).contains(i));
                }
            }
        }

        @Test
        public void testContainsOnDifferentType() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                assertFalse(filter(createUnfiltered(contents), EVEN).contains(new Object()));
            }
        }

        @Test
        public void testAddAllFailsAtomically() {
            List<Integer> toAdd = Lists.newArrayList(2, 4, 3);
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C filtered = filter(createUnfiltered(contents), EVEN);
                C filteredToModify = filter(createUnfiltered(contents), EVEN);

                try {
                    filteredToModify.addAll(toAdd);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ignored) {
                }

                assertThat(filteredToModify).containsExactlyElementsOf(filtered);
            }
        }

        @Test
        public void testAddToFilterFiltered() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C unfiltered = createUnfiltered(contents);
                C filtered1 = filter(unfiltered, EVEN);
                C filtered2 = filter(filtered1, PRIME_DIGIT);

                try {
                    filtered2.add(4);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ignored) {
                }

                try {
                    filtered2.add(3);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ignored) {
                }

                filtered2.add(2);
            }
        }

        @Test
        public void testClearFilterFiltered() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C unfiltered = createUnfiltered(contents);
                C filtered1 = filter(unfiltered, EVEN);
                C filtered2 = filter(filtered1, PRIME_DIGIT);

                C inverseFiltered = filter(createUnfiltered(contents),
                        Predicates.not(Predicates.and(EVEN, PRIME_DIGIT)));

                filtered2.clear();
                assertThat(unfiltered).containsExactlyElementsOf(inverseFiltered);
            }
        }
    }

    public static abstract class AbstractFilteredSetTest<C extends Set<Integer>>
            extends AbstractFilteredCollectionTest<C> {
        @Test
        public void testEqualsAndHashCode() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                Set<Integer> expected = Sets.newHashSet();
                for (Integer i : contents) {
                    if (EVEN.apply(i)) {
                        expected.add(i);
                    }
                }
                new EqualsTester().addEqualityGroup(expected, filter(createUnfiltered(contents), EVEN))
                        .testEquals();
            }
        }
    }

    public static abstract class AbstractFilteredSortedSetTest<C extends SortedSet<Integer>>
            extends AbstractFilteredSetTest<C> {
        @Test
        public void testFirst() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C filtered = filter(createUnfiltered(contents), EVEN);

                try {
                    Integer first = filtered.first();
                    assertFalse(filtered.isEmpty());
                    assertEquals(findMin(filtered), first);
                } catch (NoSuchElementException e) {
                    assertTrue(filtered.isEmpty());
                }
            }
        }

        private Integer findMin(C filtered) {
            Integer lastMin = Integer.MAX_VALUE;
            for (int n : filtered) {
                lastMin = Math.min(lastMin, n);
            }
            return lastMin;
        }

        @Test
        public void testLast() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                C filtered = filter(createUnfiltered(contents), EVEN);

                try {
                    Integer first = filtered.last();
                    assertFalse(filtered.isEmpty());
                    assertEquals(findMax(filtered), first);
                } catch (NoSuchElementException e) {
                    assertTrue(filtered.isEmpty());
                }
            }
        }

        private Integer findMax(C filtered) {
            Integer lastMax = Integer.MIN_VALUE;
            for (int n : filtered) {
                lastMax = Math.max(lastMax, n);
            }
            return lastMax;
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testHeadSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    assertEquals(
                            filter((C) createUnfiltered(contents).headSet(i), EVEN),
                            filter(createUnfiltered(contents), EVEN).headSet(i));
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testTailSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    assertEquals(
                            filter((C) createUnfiltered(contents).tailSet(i), EVEN),
                            filter(createUnfiltered(contents), EVEN).tailSet(i));
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSubSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    for (int j = i; j < 10; j++) {
                        assertEquals(
                                filter((C) createUnfiltered(contents).subSet(i, j), EVEN),
                                filter(createUnfiltered(contents), EVEN).subSet(i, j));
                    }
                }
            }
        }
    }

    public static abstract class AbstractFilteredNavigableSetTest
            extends AbstractFilteredSortedSetTest<NavigableSet<Integer>> {

        @Test
        public void testNavigableHeadSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    for (boolean inclusive : Lists.newArrayList(true, false)) {
                        assertEquals(
                                filter(createUnfiltered(contents).headSet(i, inclusive), EVEN),
                                filter(createUnfiltered(contents), EVEN).headSet(i, inclusive));
                    }
                }
            }
        }

        @Test
        public void testNavigableTailSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    for (boolean inclusive : Lists.newArrayList(true, false)) {
                        assertEquals(
                                filter(createUnfiltered(contents).tailSet(i, inclusive), EVEN),
                                filter(createUnfiltered(contents), EVEN).tailSet(i, inclusive));
                    }
                }
            }
        }

        @Test
        public void testNavigableSubSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                for (int i = 0; i < 10; i++) {
                    for (int j = i + 1; j < 10; j++) {
                        for (boolean fromInclusive : Lists.newArrayList(true, false)) {
                            for (boolean toInclusive : Lists.newArrayList(true, false)) {
                                NavigableSet<Integer> filterSubset = filter(
                                        createUnfiltered(contents).subSet(i, fromInclusive, j, toInclusive), EVEN);
                                NavigableSet<Integer> subsetFilter = filter(createUnfiltered(contents), EVEN)
                                        .subSet(i, fromInclusive, j, toInclusive);
                                assertEquals(filterSubset, subsetFilter);
                            }
                        }
                    }
                }
            }
        }

        @Test
        public void testDescendingSet() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
                NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

                assertThat(filtered.descendingSet()).containsExactlyElementsOf(unfiltered.descendingSet());
            }
        }

        @Test
        public void testPollFirst() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
                NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

                assertEquals(unfiltered.pollFirst(), filtered.pollFirst());
                assertEquals(unfiltered, filtered);
            }
        }

        @Test
        public void testPollLast() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
                NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

                assertEquals(unfiltered.pollLast(), filtered.pollLast());
                assertEquals(unfiltered, filtered);
            }
        }

        @Test
        public void testNavigation() {
            for (List<Integer> contents : SAMPLE_INPUTS) {
                NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
                NavigableSet<Integer> unfiltered = createUnfiltered(filtered);
                for (int i = 0; i < 10; i++) {
                    assertEquals(unfiltered.lower(i), filtered.lower(i));
                    assertEquals(unfiltered.floor(i), filtered.floor(i));
                    assertEquals(unfiltered.ceiling(i), filtered.ceiling(i));
                    assertEquals(unfiltered.higher(i), filtered.higher(i));
                }
            }
        }
    }

    // implementation tests

    public static final class IterablesFilterArrayListTest
            extends AbstractFilteredIterableTest<Iterable<Integer>> {
        @Override
        Iterable<Integer> createUnfiltered(Iterable<Integer> contents) {
            return Lists.newArrayList(contents);
        }

        @Override
        Iterable<Integer> filter(Iterable<Integer> elements, Predicate<? super Integer> predicate) {
            return Iterables.filter(elements, predicate);
        }
    }

    public static final class MoreCollectionsFilterArrayListTest
            extends AbstractFilteredCollectionTest<Collection<Integer>> {
        @Override
        Collection<Integer> createUnfiltered(Iterable<Integer> contents) {
            return Lists.newArrayList(contents);
        }

        @Override
        Collection<Integer> filter(Collection<Integer> elements, Predicate<? super Integer> predicate) {
            return MoreCollections.filter(elements, predicate);
        }
    }

    public static final class SetsFilterHashSetTest
            extends AbstractFilteredSetTest<Set<Integer>> {
        @Override
        Set<Integer> createUnfiltered(Iterable<Integer> contents) {
            return Sets.newHashSet(contents);
        }

        @Override
        Set<Integer> filter(Set<Integer> elements, Predicate<? super Integer> predicate) {
            return Sets.filter(elements, predicate);
        }
    }

    public static final class SetsFilterNavigableSetTest extends AbstractFilteredNavigableSetTest {
        @Override
        NavigableSet<Integer> createUnfiltered(Iterable<Integer> contents) {
            return Sets.newTreeSet(contents);
        }

        @Override
        NavigableSet<Integer> filter(
                NavigableSet<Integer> elements, Predicate<? super Integer> predicate) {
            return Sets.filter(elements, predicate);
        }
    }
}
