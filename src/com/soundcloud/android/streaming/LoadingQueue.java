package com.soundcloud.android.streaming;

import android.util.Log;

import java.util.ArrayList;

class LoadingQueue extends ArrayList<StreamItem> {
    public void addItem(StreamItem item, Index chunks) {
        if (item.unavailable) {
            Log.e(StreamLoader.LOG_TAG, String.format("Can't add chunks for %s: Item is not available.", item));
        } else {
            if (!contains(item)) {
                add(item);
            }
            item.index.or(chunks);
        }
    }

    public void removeItem(StreamItem item, Index chunks) {
        if (contains(item)) {
            item.index.andNot(chunks);
            if (item.index.isEmpty()) remove(item);
        }
    }

    public StreamItem head() {
        if (!isEmpty()) {
            return get(0);
        } else {
            return null;
        }
    }
}
