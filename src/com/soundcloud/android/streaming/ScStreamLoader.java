package com.soundcloud.android.streaming;

import android.content.Context;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.utils.Range;

import java.nio.ByteBuffer;
import java.util.*;

public class ScStreamLoader {

    protected NetworkConnectivityListener mConnectivityListener;
    private NetworkInfo mCurrentNetworkInfo;
    protected static final int CONNECTIVITY_MSG = 0;

    private ScStreamStorage mStorage;
    private List<ScStreamItem> mItemsNeedingHeadRequests;
    private List<ScStreamItem> mItemsNeedingPlayCountRequests;

    private int chunkSize;
    private ScStreamItem mCurrentItem;
    private int mCurrentPosition;

    private HashSet<PlayerCallback> mPlayerCallbacks;
    private ArrayList<LoadingItem> mHighPriorityQueue;
    private ArrayList<LoadingItem> mLowPriorityQueue;
    private boolean mHighPriorityConnection;
    private boolean mLowPriorityConnection;


    public void initWithStorage(Context context, ScStreamStorage storage) {
        mStorage = storage;
        chunkSize = storage.chunkSize;
        mPlayerCallbacks = new HashSet<PlayerCallback>();

        // setup connectivity listening
        mConnectivityListener = new NetworkConnectivityListener();
        mConnectivityListener.registerHandler(mConnHandler, CONNECTIVITY_MSG);
        mConnectivityListener.startListening(context);

    }

    public void cleanup(){
        mConnectivityListener.stopListening();
               mConnectivityListener.unregisterHandler(mConnHandler);
               mConnectivityListener = null;

    }

    private ByteBuffer getDataForItem(ScStreamItem item, Range byteRange) {
        Log.d(getClass().getSimpleName(), "Get Data for item " + item.toString() + " " + byteRange);

        Range chunkRange = chunkRangeForByteRange(byteRange);
        Set<Integer> missingChunksForRange = mStorage.getMissingChunksForItem(item, chunkRange);

        //If the current item changes
        if (item != mCurrentItem) {
            mItemsNeedingHeadRequests.add(item);
            // If we won't request the 0th byte (by either having it already OR jumping into the middle of a new track)
            if (!missingChunksForRange.contains(0l)) {
                countPlayForItem(item);
            }

        }

        mCurrentItem = item;
        mCurrentPosition = byteRange.location;

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

    private ByteBuffer fetchStoredDataForItem(ScStreamItem item, Range byteRange) {
        Range actualRange = byteRange;
        if (item.getContentLength() != 0){
            actualRange = byteRange.intersection(new Range(0,item.getContentLength()));
        }

        if (actualRange == null){
            Log.e(getClass().getSimpleName(), "Invalid range, outside content length. Requested range " + byteRange + " from item " + item);
            return null;
        }

        Range chunkRange = chunkRangeForByteRange(actualRange);
        byte[] data = new byte[(int) (chunkRange.length * chunkSize)];
        final long end = chunkRange.location + chunkRange.length;
        int writeIndex = 0;
        for (long chunkIndex = chunkRange.location; chunkIndex < end; chunkIndex++){
            byte[] chunkData = mStorage.getChunkData(item,chunkIndex);
            if (chunkData == null) {
                Log.e(getClass().getSimpleName(), "Error getting chunk data, aborting");
                return null;
            }
            int i = 0;
            while (i < chunkData.length){
                data[writeIndex + i] = chunkData[i];
                i++;
            }
            writeIndex += i;
        }

        if (data.length < actualRange.length){
            return ByteBuffer.wrap(data, actualRange.location - (chunkRange.location * chunkSize), actualRange.length).slice().asReadOnlyBuffer();
        } else {
            return ByteBuffer.wrap(data);
        }
    }

    private void processQueues() {

        if (!isOnline()) return;

        if (mItemsNeedingHeadRequests.size() > 0){
            for (ScStreamItem item : mItemsNeedingHeadRequests){
                // start head connection for item
            }
            mItemsNeedingHeadRequests.clear();
        }

         if (mItemsNeedingPlayCountRequests.size() > 0){
            for (ScStreamItem item : mItemsNeedingPlayCountRequests){
                // start play count connection for item
            }
            mItemsNeedingPlayCountRequests.clear();
        }

        processHighPriorityQueue();
        if (!mHighPriorityConnection){
            processLowPriorityQueue();
        }
    }

    private void processHighPriorityQueue() {
        if (mHighPriorityConnection) return;
        if (mHighPriorityQueue.size() == 0) return;

        //Look if it is the current LowPriority Chunk.
        if (mLowPriorityConnection) {
            /*
             TODO cancel low priority connection and re-add it to the queue
             https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L789
              */

        }

        for (LoadingItem highPriorityItem : mHighPriorityQueue) {
            ScStreamItem item = highPriorityItem.scStreamItem;
            if (!item.enabled) {
                mHighPriorityQueue.remove(highPriorityItem);
            } else if (item.getContentLength() != 0) {
                /*
                 TODO download chunk indexes in loading item
                 https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L806
                  */
            } else {
                /*
                 TODO do a head request to get content length
                 https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L818
                  */
            }

        }
    }

    private void processLowPriorityQueue() {
        if (mLowPriorityConnection) return;
        if (mLowPriorityQueue.size() == 0) return;

        for (LoadingItem highPriorityItem : mHighPriorityQueue) {
            ScStreamItem item = highPriorityItem.scStreamItem;
            if (!item.enabled) {
                mHighPriorityQueue.remove(highPriorityItem);
            } else if (item.getContentLength() != 0) {
                /*
                 TODO download chunk indexes in loading item
                 https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L840
                  */
            } else {
                /*
                 TODO do a head request to get content length
                 https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L851
                  */
            }

        }
    }

    private void updateLowPriorityQueue() {
        /*
        TODO prefetching. Not sure how much we want to do yet
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L573
         */
    }

    private void addItem(ScStreamItem item, Set<Integer> chunks, List<LoadingItem> queue) {
        if (!item.enabled) {
            Log.e(getClass().getSimpleName(), "Can't add chunks for %@: Item is disabled." + item.getURLHash());
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
        TODO request necessary range for a playcount
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L924
         */
    }

    private Range chunkRangeForByteRange(Range byteRange) {
        return new Range(byteRange.location / chunkSize,
                (int) Math.ceil((double) ((byteRange.location % chunkSize) + byteRange.length) / (double) chunkSize));
    }

    private void onDataReceived() {
        Log.d(getClass().getSimpleName(), "");

    }

    private void storeData(byte[] data, int chunk, ScStreamItem item) {
        Log.d(getClass().getSimpleName(), "");

    }

    private void fulfillPlayerCallbacks(){
        ArrayList<PlayerCallback> fulfilledCallbacks = new ArrayList<PlayerCallback>();
        for (PlayerCallback playerCallback : mPlayerCallbacks) {
            ScStreamItem item = playerCallback.scStreamItem;
            Range chunkRange = chunkRangeForByteRange(playerCallback.byteRange);
            HashSet<Integer> missingIndexes = mStorage.getMissingChunksForItem(item, chunkRange);
            if (missingIndexes.size() == 0) {
                fulfilledCallbacks.add(playerCallback);
            }
        }

        for (PlayerCallback playerCallback : fulfilledCallbacks) {
            // TODO notify that data was loaded
            mPlayerCallbacks.remove(playerCallback);
        }
    }

    private boolean isOnline() {
        if (mConnectivityListener == null) return false;
        mCurrentNetworkInfo = mConnectivityListener.getNetworkInfo();
        return mCurrentNetworkInfo != null && mCurrentNetworkInfo.isConnected();
    }

    private Handler mConnHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                   // TODO process queues
                    break;
            }
        }
    };

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
}