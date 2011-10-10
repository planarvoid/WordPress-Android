package com.soundcloud.android.streaming;

import android.util.Log;
import com.soundcloud.android.utils.Range;

import java.lang.reflect.Array;
import java.util.*;

public class ScStreamLoader {

    private ScStreamStorage mStorage;
    private List<ScStreamItem> mItemsNeedingHeadRequests;

    private long chunkSize;
    private ScStreamItem mCurrentItem;
    private long mCurrentPosition;

    private HashSet<PlayerCallback> mPlayerCallbacks;
    private ArrayList<LoadingItem> mHighPriorityQueue;
    private ArrayList<LoadingItem> mLowPriorityQueue;

    private class PlayerCallback {
        ScStreamItem scStreamItem;
        Range byteRange;

        public PlayerCallback(ScStreamItem scStreamItem, Range byteRange) {
            this.scStreamItem = scStreamItem;
            this.byteRange = byteRange;
        }
    }

    private class LoadingItem {
        ScStreamItem scStreamItem;
        Set indexes;

        public LoadingItem(ScStreamItem item) {
            this.scStreamItem = item;
        }

        public LoadingItem(ScStreamItem scStreamItem, Set indexes) {
            this(scStreamItem);
            this.indexes = indexes;
        }
    }

    public void initWithStorage(ScStreamStorage storage) {
        mStorage = storage;
        chunkSize = storage.chunkSize;
        mPlayerCallbacks = new HashSet<PlayerCallback>();
    }

    private byte[] getDataForItem(ScStreamItem item, Range byteRange) {
        Log.d(getClass().getSimpleName(), "Get Data for item " + item.toString() + " " + byteRange);

        Range chunkRange = chunkRangeForByteRange(byteRange);
        Set<Long> missingChunksForRange = mStorage.getMissingChunksForItem(item, chunkRange);

        //If the current item changes
        if (item != mCurrentItem) {
            mItemsNeedingHeadRequests.add(item);
            // If we won't request the 0th byte (by either having it already OR jumping into the middle of a new track)
            if (!missingChunksForRange.contains(0l)) {
                countPlayForItem(item);
            }

        }

        mCurrentItem = item;
        mCurrentPosition = byteRange.start;

        if (missingChunksForRange.size() > 0) {
            mPlayerCallbacks.add(new PlayerCallback(item, byteRange));
            addItem(item, missingChunksForRange, mHighPriorityQueue);
            updateLowPriorityQueue();
            processQueues();
            return null;
        } else {
            Log.d(getClass().getSimpleName(), "Serving item from storage");
            return fetchStoredDataForItem(item, byteRange);
        }
    }

    private byte[] fetchStoredDataForItem(ScStreamItem item, Range byteRange) {
        /*
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L868
         */
        return new byte[0];
    }

    private void processQueues() {
        /*
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L722
         */
    }

    private void updateLowPriorityQueue() {
        /*
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L573
         */
    }

    private void addItem(ScStreamItem item, Set<Long> chunks, List<LoadingItem> queue) {
        if (!item.enabled) {
            Log.e("asdf", "Can't add chunks for %@: Item is disabled." + item.getURLHash());
            return;
        }

        LoadingItem loadingItem = null;
        for (LoadingItem candidate : queue) {
            if (candidate.scStreamItem.equals(item)) {
                loadingItem = candidate;
                break;
            }
        }

        if (loadingItem == null) {
            loadingItem = new LoadingItem(item);
            queue.add(loadingItem);
        }
        loadingItem.indexes.addAll(chunks);
    }

    private void removeItem(ScStreamItem item, Set<Long> chunks, List<LoadingItem> queue) {
        LoadingItem loadingItem = null;
        for (LoadingItem candidate : queue) {
            if (candidate.scStreamItem.equals(item)) {
                loadingItem = candidate;
                break;
            }
        }

        //Remove
        if (loadingItem != null) {
            loadingItem.indexes.removeAll(chunks);
            if (loadingItem.indexes.size() == 0) queue.remove(loadingItem);

        }
    }

    private void countPlayForItem(ScStreamItem item) {
        if (item == null) return;
        /*
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L924
         */
    }

    private Range chunkRangeForByteRange(Range byteRange) {
        return new Range(byteRange.start / chunkSize,
                (long) Math.ceil((double) ((byteRange.start % chunkSize) + byteRange.length) / (double) chunkSize));
    }

    private void onDataReceived() {
        Log.d(getClass().getSimpleName(), "");

    }

    private void storeData(byte[] data, int chunk, ScStreamItem item) {
        Log.d(getClass().getSimpleName(), "");

    }
}