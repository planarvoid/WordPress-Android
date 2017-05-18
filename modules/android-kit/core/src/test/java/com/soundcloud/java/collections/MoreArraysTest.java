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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Unit test for {@code MoreArrays}.
 *
 * @author Kevin Bourrillion
 */
public class MoreArraysTest {

    @Test
    public void testNewArray_fromClass_Empty() {
        String[] empty = MoreArrays.newArray(String.class, 0);
        assertEquals(String[].class, empty.getClass());
        assertEquals(0, empty.length);
    }

    @Test
    public void testNewArray_fromClass_Nonempty() {
        String[] array = MoreArrays.newArray(String.class, 2);
        assertEquals(String[].class, array.getClass());
        assertEquals(2, array.length);
        assertNull(array[0]);
    }

    @Test
    public void testNewArray_fromClass_OfArray() {
        String[][] array = MoreArrays.newArray(String[].class, 1);
        assertEquals(String[][].class, array.getClass());
        assertEquals(1, array.length);
        assertNull(array[0]);
    }

    @Test
    public void testNewArray_fromArray_Empty() {
        String[] in = new String[0];
        String[] empty = MoreArrays.newArray(in, 0);
        assertEquals(0, empty.length);
    }

    @Test
    public void testNewArray_fromArray_Nonempty() {
        String[] array = MoreArrays.newArray(new String[0], 2);
        assertEquals(String[].class, array.getClass());
        assertEquals(2, array.length);
        assertNull(array[0]);
    }

    @Test
    public void testNewArray_fromArray_OfArray() {
        String[][] array = MoreArrays.newArray(new String[0][0], 1);
        assertEquals(String[][].class, array.getClass());
        assertEquals(1, array.length);
        assertNull(array[0]);
    }

    @Test
    public void testConcatEmptyEmpty() {
        String[] result
                = MoreArrays.concat(new String[0], new String[0], String.class);
        assertEquals(String[].class, result.getClass());
        assertEquals(0, result.length);
    }

    @Test
    public void testConcatEmptyNonempty() {
        String[] result = MoreArrays.concat(
                new String[0], new String[]{"a", "b"}, String.class);
        assertEquals(String[].class, result.getClass());
        assertThat(asList(result)).containsExactly("a", "b");
    }

    @Test
    public void testConcatNonemptyEmpty() {
        String[] result = MoreArrays.concat(
                new String[]{"a", "b"}, new String[0], String.class);
        assertEquals(String[].class, result.getClass());
        assertThat(asList(result)).containsExactly("a", "b");
    }

    @Test
    public void testConcatBasic() {
        String[] result = MoreArrays.concat(
                new String[]{"a", "b"}, new String[]{"c", "d"}, String.class);
        assertEquals(String[].class, result.getClass());
        assertThat(asList(result)).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void testConcatWithMoreGeneralType() {
        Serializable[] result
                = MoreArrays.concat(new String[0], new String[0], Serializable.class);
        assertEquals(Serializable[].class, result.getClass());
    }

    @Test
    public void testPrependZeroElements() {
        String[] result = MoreArrays.concat("foo", new String[]{});
        assertThat(asList(result)).containsExactly("foo");
    }

    @Test
    public void testPrependOneElement() {
        String[] result = MoreArrays.concat("foo", new String[]{"bar"});
        assertThat(asList(result)).containsExactly("foo", "bar");
    }

    @Test
    public void testPrependTwoElements() {
        String[] result = MoreArrays.concat("foo", new String[]{"bar", "baz"});
        assertThat(asList(result)).containsExactly("foo", "bar", "baz");
    }

    @Test
    public void testAppendZeroElements() {
        String[] result = MoreArrays.concat(new String[]{}, "foo");
        assertThat(asList(result)).containsExactly("foo");
    }

    @Test
    public void testAppendOneElement() {
        String[] result = MoreArrays.concat(new String[]{"foo"}, "bar");
        assertThat(asList(result)).containsExactly("foo", "bar");
    }

    @Test
    public void testAppendTwoElements() {
        String[] result = MoreArrays.concat(new String[]{"foo", "bar"}, "baz");
        assertThat(asList(result)).containsExactly("foo", "bar", "baz");
    }

    @Test
    public void testEmptyArrayToEmpty() {
        doTestNewArrayEquals(new Object[0], 0);
    }

    @Test
    public void testEmptyArrayToNonEmpty() {
        checkArrayEquals(new Long[5], MoreArrays.newArray(new Long[0], 5));
    }

    @Test
    public void testNonEmptyToShorter() {
        checkArrayEquals(new String[9], MoreArrays.newArray(new String[10], 9));
    }

    @Test
    public void testNonEmptyToSameLength() {
        doTestNewArrayEquals(new String[10], 10);
    }

    @Test
    public void testNonEmptyToLonger() {
        checkArrayEquals(new String[10],
                MoreArrays.newArray(new String[]{"a", "b", "c", "d", "e"}, 10));
    }

    @Test
    public void shouldReturnFirstNonNullItem() {
        assertThat(MoreArrays.firstNonNull(1, null, 2)).isEqualTo(1);
        assertThat(MoreArrays.firstNonNull(null, 1, 2)).isEqualTo(1);
        assertThat(MoreArrays.firstNonNull(1, 2, null)).isEqualTo(1);
        assertThat(MoreArrays.firstNonNull(null, null, 1)).isEqualTo(1);
        assertThat(MoreArrays.firstNonNull(null, null, null)).isNull();
        assertThat(MoreArrays.firstNonNull()).isNull();
    }

    private static void checkArrayEquals(Object[] expected, Object[] actual) {
        assertTrue("expected(" + expected.getClass() + "): " + Arrays.toString(expected)
                        + " actual(" + actual.getClass() + "): " + Arrays.toString(actual),
                arrayEquals(expected, actual));
    }

    private static boolean arrayEquals(Object[] array1, Object[] array2) {
        assertSame(array1.getClass(), array2.getClass());
        return Arrays.equals(array1, array2);
    }

    private static void doTestNewArrayEquals(Object[] expected, int length) {
        checkArrayEquals(expected, MoreArrays.newArray(expected, length));
    }
}
