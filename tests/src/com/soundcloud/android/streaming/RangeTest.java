package com.soundcloud.android.streaming;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RangeTest {
    @Test
    public void testConstruction() throws Exception {
        Range r = Range.from(0, 10);
        assertThat(r.location, equalTo(0));
        assertThat(r.length, equalTo(10));
        assertThat(r.end(), equalTo(10));
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
        assertThat(r.end(), equalTo(30));
    }

    @Test
    public void testChunkRange() throws Exception {
        Range r = Range.from(1025, 2555);
        Range chunk = r.chunkRange(1024);

        assertThat(chunk.location, is(1));
        assertThat(chunk.length, is(3));
    }
}
