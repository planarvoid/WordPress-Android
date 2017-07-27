/*
 * Copyright (C) 2011 The Guava Authors
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

package com.soundcloud.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.soundcloud.java.collections.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tester for {@link Equivalence} relationships between groups of objects.
 *
 * <p>
 * To use, create a new {@link EquivalenceTester} and add equivalence groups
 * where each group contains objects that are supposed to be equal to each
 * other. Objects of different groups are expected to be unequal. For example:
 *
 * <pre>
 * {@code
 * EquivalenceTester.of(someStringEquivalence)
 *     .addEquivalenceGroup("hello", "h" + "ello")
 *     .addEquivalenceGroup("world", "wor" + "ld")
 *     .test();
 * }
 * </pre>
 *
 * <p>
 * Note that testing {@link Object#equals(Object)} is more simply done using
 * the {@link EqualsTester}. It includes an extra test against an instance of an
 * arbitrary class without having to explicitly add another equivalence group.
 *
 * @author Gregory Kick
 * @since 10.0
 *
 * TODO(gak): turn this into a test suite so that each test can fail
 * independently
 */
final class EquivalenceTester<T> {
    private static final int REPETITIONS = 3;

    private final Equivalence<? super T> equivalence;
    private final RelationshipTester<T> delegate;
    private final List<T> items = new ArrayList<>();

    private EquivalenceTester(Equivalence<? super T> equivalence) {
        if (equivalence == null) {
            throw new IllegalArgumentException("Equivalence must not be null");
        }
        this.equivalence = equivalence;
        this.delegate = new RelationshipTester<T>(
                equivalence, "equivalent", "hash", new RelationshipTester.ItemReporter());
    }

    public static <T> EquivalenceTester<T> of(Equivalence<? super T> equivalence) {
        return new EquivalenceTester<>(equivalence);
    }

    /**
     * Adds a group of objects that are supposed to be equivalent to each other
     * and not equivalent to objects in any other equivalence group added to this
     * tester.
     */
    public EquivalenceTester<T> addEquivalenceGroup(T... groups) {
        addEquivalenceGroup(Arrays.asList(groups));
        return this;
    }

    public EquivalenceTester<T> addEquivalenceGroup(Iterable<T> group) {
        delegate.addRelatedGroup(group);
        items.addAll(Lists.newArrayList(group.iterator()));
        return this;
    }

    /**
     * Run tests on equivalence methods, throwing a failure on an invalid test
     */
    public EquivalenceTester<T> test() {
        for (int run = 0; run < REPETITIONS; run++) {
            testItems();
            delegate.test();
        }
        return this;
    }

    private void testItems() {
        for (T item : items) {
      /*
       * TODO(cpovirk): consider no longer running these equivalent() tests on every Equivalence,
       * since the Equivalence base type now implements this logic itself
       */
            assertTrue(item + " must be inequivalent to null", !equivalence.equivalent(item, null));
            assertTrue("null must be inequivalent to " + item, !equivalence.equivalent(null, item));
            assertTrue(item + " must be equivalent to itself", equivalence.equivalent(item, item));
            assertEquals("the hash of " + item + " must be consistent", equivalence.hash(item),
                    equivalence.hash(item));
        }
    }
}
