package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class RangeTest {
    @Test
    public void testConstruction() throws Exception {
        Range r = Range.from(0, 10);

        expect(r.location).toBe(0);
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

        expect(chunk.location).toBe(1);
        expect(chunk.length).toBe(3);
    }
}
