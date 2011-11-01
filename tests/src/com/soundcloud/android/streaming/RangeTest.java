package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.util.Iterator;

public class RangeTest {
    @Test
    public void testConstruction() throws Exception {
        Range r = Range.from(0, 10);

        expect(r.start).toBe(0);
        expect(r.length).toBe(10);
        expect(r.end()).toBe(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnforceValidBounds() throws Exception {
        Range.from(-1, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnforceValidBounds2() throws Exception {
        Range.from(0, -1);
    }

    @Test
    public void testEnd() throws Exception {
        Range r = Range.from(10, 20);
        expect(r.end()).toBe(30);
    }

    @Test
    public void testChunkRange() throws Exception {
        Range r = Range.from(1025, 2555);
        Range chunk = r.chunkRange(1024);

        expect(chunk.start).toBe(1);
        expect(chunk.length).toBe(3);
    }

    @Test
    public void testIntersection() throws Exception {
        expect(Range.from(10, 40).intersection(Range.from(0, 60))).toEqual(Range.from(10,40));
        expect(Range.from(20,10).intersection(Range.from(0,5))).toBeNull();
    }

    @Test
    public void shouldSupportEquals() throws Exception {
        expect(Range.from(0, 10)).toEqual(Range.from(0, 10));
        expect(Range.from(0, 10)).not.toEqual(Range.from(1, 10));
    }

    @Test
    public void moveStart() throws Exception {
        expect(Range.from(0, 10).moveStart(10)).toEqual(Range.from(10, 10));
    }

    @Test
    public void shouldSupportIteratable() throws Exception {
        Iterator<Integer> it = Range.from(1, 3).iterator();
        expect(it.next()).toBe(1);
        expect(it.next()).toBe(2);
        expect(it.next()).toBe(3);
        expect(it.hasNext()).toBe(false);
    }
}
