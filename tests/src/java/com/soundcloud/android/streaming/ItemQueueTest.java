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
        expect(q.addItem(new StreamItem("https://api.soundcloud.com/tracks/12345/stream"), Index.create(1))).toBeTrue();
    }

    @Test
    public void shouldNotAddItemsWithoutChunksToDownload() throws Exception {
        ItemQueue q = new ItemQueue();
        expect(q.addItem(new StreamItem("https://api.soundcloud.com/tracks/12345/stream"), Index.empty())).toBeFalse();
    }

    @Test
    public void testRemoveIfCompletedItem() throws Exception {
        ItemQueue q = new ItemQueue();
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");

        expect(q.addItem(item, Index.create(1, 3))).toBeTrue();
        expect(q.addItem(item, Index.create(1, 3))).toBeFalse();
        expect(q.removeIfCompleted(item, Index.create(1))).toBeFalse();
        expect(q.removeIfCompleted(item, Index.create(3))).toBeTrue();
        expect(q.removeIfCompleted(item, Index.empty())).toBeFalse();
        expect(q.isEmpty()).toBeTrue();
    }

    @Test
    public void testIteratorShouldIterateOverCopyOfQueue() throws Exception {
        ItemQueue q = new ItemQueue();
        q.addItem(new StreamItem("https://api.soundcloud.com/tracks/1/stream"), Index.create(1));
        q.addItem(new StreamItem("https://api.soundcloud.com/tracks/2/stream"), Index.create(1));
        q.addItem(new StreamItem("https://api.soundcloud.com/tracks/2/stream"), Index.create(1));

        for (StreamItem i : q) {
            q.addItem(new StreamItem("https://api.soundcloud.com/tracks/0/stream"), Index.create(0));
            q.removeIfCompleted(i, Index.create(1));
        }

        expect(q.size()).toBe(1);
        expect(q.head()).toEqual(new StreamItem("https://api.soundcloud.com/tracks/0/stream"));
    }

    @Test
    public void shouldNotAddUnavailableItemToQueue() throws Exception {
        ItemQueue q = new ItemQueue();
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        item.markUnavailable(404);
        expect(q.addItem(item, Index.create(1))).toBeFalse();
    }
}
