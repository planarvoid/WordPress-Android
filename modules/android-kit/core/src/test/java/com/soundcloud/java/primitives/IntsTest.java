package com.soundcloud.java.primitives;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IntsTest {

    private static final int[] VALUES =
            {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};


    @Test
    public void testCheckedCast() {
        for (int value : VALUES) {
            assertEquals(value, Ints.checkedCast((long) value));
        }
        assertCastFails(Integer.MAX_VALUE + 1L);
        assertCastFails(Integer.MIN_VALUE - 1L);
        assertCastFails(Long.MAX_VALUE);
        assertCastFails(Long.MIN_VALUE);
    }

    @Test
    public void testSaturatedCast() {
        for (int value : VALUES) {
            assertEquals(value, Ints.saturatedCast((long) value));
        }
        assertEquals(Integer.MAX_VALUE, Ints.saturatedCast(Integer.MAX_VALUE + 1L));
        assertEquals(Integer.MIN_VALUE, Ints.saturatedCast(Integer.MIN_VALUE - 1L));
        assertEquals(Integer.MAX_VALUE, Ints.saturatedCast(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Ints.saturatedCast(Long.MIN_VALUE));
    }

    @Test
    public void shouldGroupEvenNumberIntoBatches() {
        assertArrayEquals(new int[]{}, Ints.inGroupsOf(50, 0));
        assertArrayEquals(new int[]{50, 50}, Ints.inGroupsOf(50, 100));
        assertArrayEquals(new int[]{25, 25, 25, 25}, Ints.inGroupsOf(25, 100));
    }

    @Test
    public void shouldGroupOddNumberIntoBatches() {
        assertArrayEquals(new int[]{50, 45}, Ints.inGroupsOf(50, 95));
        assertArrayEquals(new int[]{50, 50, 5}, Ints.inGroupsOf(50, 105));
        assertArrayEquals(new int[]{25, 25, 25, 20}, Ints.inGroupsOf(25, 95));
    }

    private static void assertCastFails(long value) {
        try {
            Ints.checkedCast(value);
            fail("Cast to int should have failed: " + value);
        } catch (IllegalArgumentException ex) {
            assertTrue(value + " not found in exception text: " + ex.getMessage(),
                    ex.getMessage().contains(String.valueOf(value)));
        }
    }

}
