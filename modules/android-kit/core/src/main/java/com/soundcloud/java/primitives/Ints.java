package com.soundcloud.java.primitives;

import java.util.Arrays;

public final class Ints {

    public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

    /**
     * Returns the {@code int} value that is equal to {@code value}, if possible.
     *
     * @param value any value in the range of the {@code int} type
     * @return the {@code int} value that equals {@code value}
     * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE}
     *                                  or less than {@link Integer#MIN_VALUE}
     */
    public static int checkedCast(long value) {
        int result = (int) value;
        if (result != value) {
            // don't use checkArgument here, to avoid boxing
            throw new IllegalArgumentException("Out of range: " + value);
        }
        return result;
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the
     * {@code int} type, {@link Integer#MAX_VALUE} if it is too large,
     * or {@link Integer#MIN_VALUE} if it is too small
     */
    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Segments an integer into groups of a given size. The last group might be smaller than
     * the requested size if the source number doesn't divide without a remainder.
     * Segmenting 0 yields an empty array.
     * <p>
     * Examples:
     * <pre>
     * <code>inGroupsOf(2, 0) ==> {}</code>
     * <code>inGroupsOf(2, 4) ==> {2, 2}</code>
     * <code>inGroupsOf(1, 4) ==> {1, 1, 1, 1}</code>
     * <code>inGroupsOf(2, 5) ==> {2, 2, 2, 1}</code>
     * </pre>
     * </p>
     *
     * @param size   the group size into which the number gets broken down
     * @param number the number to break down into groups
     */
    public static int[] inGroupsOf(int size, int number) {
        if (!(size > 0)) {
            throw new IllegalArgumentException();
        }
        final int count = number / size;
        final int remainder = number % size;
        final int[] groups = new int[count + (remainder > 0 ? 1 : 0)];
        Arrays.fill(groups, size);
        if (remainder > 0) {
            groups[groups.length - 1] = remainder;
        }
        return groups;
    }

    private Ints() {
        // no instances
    }
}
