package com.soundcloud.android.streaming;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class ItemQueueTest {
    @Test
    public void testAddItem() throws Exception {
        ItemQueue q = new ItemQueue();

        expect(q.addItem(new StreamItem("/foo"), Index.empty())).toBeTrue();
        expect(q.addItem(new StreamItem("/foo"), Index.empty())).toBeFalse();
    }

    @Test
    public void testRemoveIfCompletedItem() throws Exception {
        ItemQueue q = new ItemQueue();
        StreamItem item = new StreamItem("/foo");

        expect(q.addItem(item, Index.create(1,3))).toBeTrue();
        expect(q.addItem(item, Index.create(1,3))).toBeFalse();
        expect(q.removeIfCompleted(item, Index.create(1))).toBeFalse();
        expect(q.removeIfCompleted(item, Index.create(3))).toBeTrue();
        expect(q.removeIfCompleted(item, Index.empty())).toBeFalse();
        expect(q.isEmpty()).toBeTrue();
    }

    @Test
    public void testIteratorShouldIterateOverCopyOfQueue() throws Exception {
        ItemQueue q = new ItemQueue();
        q.addItem(new StreamItem("/1"), Index.empty());
        q.addItem(new StreamItem("/2"), Index.empty());
        q.addItem(new StreamItem("/3"), Index.empty());

        for (StreamItem i : q) {
            q.addItem(new StreamItem("/0"), Index.empty());
            q.removeIfCompleted(i, Index.empty());
        }

        expect(q.size()).toBe(1);
        expect(q.head()).toEqual(new StreamItem("/0"));
    }
}
