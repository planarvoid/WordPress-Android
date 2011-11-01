package com.soundcloud.android.streaming;

import com.soundcloud.api.Stream;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ItemQueue implements Iterable<StreamItem> {
    private final List<StreamItem> mItems;

    public ItemQueue() {
        this(new ArrayList<StreamItem>());
    }

    public ItemQueue(List<StreamItem> mItems) {
        this.mItems = mItems;
    }

    public boolean addItem(StreamItem item, Index chunks) {
        if (item.unavailable) {
            Log.e(StreamLoader.LOG_TAG, String.format("Can't add chunks for %s: Item is not available.", item));
            return false;
        } else {
            item.index.or(chunks);
            return !mItems.contains(item) && mItems.add(item);
        }
    }

    public boolean add(StreamItem item) {
        return mItems.add(item);
    }

    public boolean remove(StreamItem item) {
        return mItems.remove(item);
    }

    public boolean removeIfCompleted(StreamItem item, Index chunks) {
        if (mItems.contains(item)) {
            item.index.andNot(chunks);
            return item.index.isEmpty() && mItems.remove(item);
        } else return false;
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
