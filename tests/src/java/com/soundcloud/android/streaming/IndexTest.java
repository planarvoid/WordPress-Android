package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

public class IndexTest {
    @Test
    public void testIterator() throws Exception {
        Index i = Index.create(10,20,5);
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
    public void testIteratorRemove() throws Exception {
        Index i = Index.create(10,20,5);

        expect(i.size()).toBe(3);

        Iterator<Integer> it = i.iterator();
        expect(it.hasNext()).toBeTrue();
        expect(it.next()).toBe(5);

        it.remove();

        expect(i.size()).toBe(2);
        expect(i.get(5)).toBeFalse();
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
