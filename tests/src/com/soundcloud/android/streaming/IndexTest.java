package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.util.Iterator;

public class IndexTest {
    @Test
    public void testIterator() throws Exception {
        Index i = new Index();
        i.set(10);
        i.set(20);
        i.set(5);

        Iterator<Integer> it = i.iterator();

        expect(it.hasNext()).toBeTrue();
        expect(it.next()).toBe(5);
        expect(it.hasNext()).toBeTrue();
        expect(it.next()).toBe(10);
        expect(it.hasNext()).toBeTrue();
        expect(it.next()).toBe(20);
        expect(it.hasNext()).toBeFalse();
    }

    @Test
    public void testSize() throws Exception {
        expect(Index.create(1, 2, 3).size()).toBe(3);
        expect(Index.empty().size()).toBe(0);
    }

    @Test
    public void testFirst() throws Exception {
       expect(Index.create(1, 2, 3).first()).toBe(1);
    }
}
