package com.soundcloud.android.playback.streaming;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class ItemQueue implements Iterable<StreamItem> {
    private final List<StreamItem> items;

    public ItemQueue() {
        this(new ArrayList<StreamItem>());
    }

    public ItemQueue(List<StreamItem> items) {
        this.items = Collections.synchronizedList(items);
    }

    public boolean addItem(StreamItem item, Index chunksToDownload) {
        if (item.isAvailable()) {
            item.missingChunks.or(chunksToDownload);
            if (!item.missingChunks.isEmpty() /* only add to q if there's something to download */
                    && !items.contains(item)) {
                items.add(0, item);
                return true;

            } else {
                return false;
            }
        } else {
            Log.e(StreamLoader.LOG_TAG, String.format("Can't add chunks for %s: Item is not available.", item));
            return false;
        }
    }

    public boolean removeIfCompleted(StreamItem item, Index newChunks) {
        if (items.contains(item)) {
            item.missingChunks.andNot(newChunks);
            return item.missingChunks.isEmpty() && items.remove(item);
        } else {
            return false;
        }
    }

    public boolean contains(StreamItem item) {
        return items.contains(item);
    }

    public boolean add(StreamItem item) {
        return !items.contains(item) && items.add(item);
    }

    public boolean remove(StreamItem item) {
        return items.remove(item);
    }

    public StreamItem head() {
        if (!items.isEmpty()) {
            return items.get(0);
        } else {
            return null;
        }
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    @Override
    public Iterator<StreamItem> iterator() {
        // provide a copy of the data so we can manipulate the queue while iterating over it
        return new ArrayList<StreamItem>(items).iterator();
    }
}
