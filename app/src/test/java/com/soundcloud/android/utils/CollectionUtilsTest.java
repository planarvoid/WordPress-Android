package com.soundcloud.android.utils;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class CollectionUtilsTest {

    @Test
    public void testIteratorElementsEqual() {
        Iterable<?> a;
        Iterable<?> b;

        // Base case.
        a = new ArrayList<>();
        b = Collections.emptySet();
        assertTrue(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));

        // A few elements.
        a = asList(4, 8, 15, 16, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertTrue(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));

        // The same, but with nulls.
        a = asList(4, 8, null, 16, 23, 42);
        b = asList(4, 8, null, 16, 23, 42);
        assertTrue(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));

        // An element differs.
        a = asList(4, 8, 15, 12, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));

        // null versus non-null.
        a = asList(4, 8, 15, null, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(CollectionUtils.elementsEqual(b.iterator(), a.iterator()));

        // Different lengths.
        a = asList(4, 8, 15, 16, 23);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(CollectionUtils.elementsEqual(b.iterator(), a.iterator()));

        // Different lengths, one is empty.
        a = Collections.emptySet();
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a.iterator(), b.iterator()));
        assertFalse(CollectionUtils.elementsEqual(b.iterator(), a.iterator()));
    }

    @Test
    public void testIterableElementsEqual() throws Exception {
        Iterable<?> a;
        Iterable<?> b;

        // A few elements.
        a = asList(4, 8, 15, 16, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertTrue(CollectionUtils.elementsEqual(a, b));

        // An element differs.
        a = asList(4, 8, 15, 12, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a, b));

        // null versus non-null.
        a = asList(4, 8, 15, null, 23, 42);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a, b));
        assertFalse(CollectionUtils.elementsEqual(b, a));

        // Different lengths.
        a = asList(4, 8, 15, 16, 23);
        b = asList(4, 8, 15, 16, 23, 42);
        assertFalse(CollectionUtils.elementsEqual(a, b));
        assertFalse(CollectionUtils.elementsEqual(b, a));
    }


}