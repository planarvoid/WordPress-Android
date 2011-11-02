package com.soundcloud.android.streaming;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ItemQueue implements Iterable<StreamItem> {
    private final List<StreamItem> mItems;

    public ItemQueue() {
        this(new ArrayList<StreamItem>());
    }

    public ItemQueue(List<StreamItem> items) {
        this.mItems = items;
    }

    public boolean addItem(StreamItem item, Index chunksToDownload) {
        if (item.unavailable) {
            Log.e(StreamLoader.LOG_TAG, String.format("Can't add chunks for %s: Item is not available.", item));
            return false;
        } else {
            item.chunksToDownload.or(chunksToDownload);
            return !item.chunksToDownload.isEmpty() /* only add to q if there's something to download */
                && !mItems.contains(item)
                && mItems.add(item);
        }
    }

    public boolean removeIfCompleted(StreamItem item, Index newChunks) {
        if (mItems.contains(item)) {
            item.chunksToDownload.andNot(newChunks);
            return item.chunksToDownload.isEmpty() && mItems.remove(item);
        } else return false;
    }

    public boolean contains(StreamItem item) {
        return mItems.contains(item);
    }

    public boolean add(StreamItem item) {
        return !mItems.contains(item) && mItems.add(item);
    }

    public boolean remove(StreamItem item) {
        return mItems.remove(item);
    }

    public StreamItem head() {
        if (!mItems.isEmpty()) {
            return mItems.get(0);
        } else {
            return null;
        }
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public int size() {
        return mItems.size();
    }

    @Override
    public Iterator<StreamItem> iterator() {
        // provide a copy of the data so we can manipulate the queue while iterating over it
        return new ArrayList<StreamItem>(mItems).iterator();
    }
}
