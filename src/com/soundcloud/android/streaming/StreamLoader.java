package com.soundcloud.android.streaming;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.content.Context;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class StreamLoader {

    protected NetworkConnectivityListener mConnectivityListener;
    private NetworkInfo mCurrentNetworkInfo;
    protected static final int CONNECTIVITY_MSG = 0;

    private Context mContext;
    private StreamStorage mStorage;
    private List<StreamItem> mItemsNeedingHeadRequests;
    private List<StreamItem> mItemsNeedingPlayCountRequests;

    private int chunkSize;
    private StreamItem mCurrentItem;
    private int mCurrentPosition;

    private HashSet<PlayerCallback> mPlayerCallbacks;
    private ArrayList<LoadingItem> mHighPriorityQueue;
    private ArrayList<LoadingItem> mLowPriorityQueue;
    private DataTask mHighPriorityConnection;
    private boolean mLowPriorityConnection;
    private static final String STREAM_ITEM_RANGE_LOADED = "com.soundcloud.android.streaming.streamitemrangeloaded";

    private Set<HeadTask> mHeadTasks;

    private StreamHandler mDataHandler;
    private StreamHandler mHeadHandler;
    private Handler mResultHandler;


    public StreamLoader(Context context, StreamStorage storage) {
        mContext = context;
        mStorage = storage;
        chunkSize = storage.chunkSize;
        mPlayerCallbacks = new HashSet<PlayerCallback>();

        // setup connectivity listening
        mConnectivityListener = new NetworkConnectivityListener();
        mConnectivityListener.registerHandler(mConnHandler, CONNECTIVITY_MSG);
        mConnectivityListener.startListening(context);

        mItemsNeedingHeadRequests = new ArrayList<StreamItem>();
        mItemsNeedingPlayCountRequests = new ArrayList<StreamItem>();

        mHeadTasks = new HashSet<HeadTask>();

        HandlerThread dataThread = new HandlerThread(getClass().getSimpleName(), THREAD_PRIORITY_BACKGROUND);
        dataThread.start();
        mDataHandler = new StreamHandler(dataThread.getLooper());

        HandlerThread contentLengthThread = new HandlerThread(getClass().getSimpleName(), THREAD_PRIORITY_BACKGROUND);
        contentLengthThread.start();
        mHeadHandler = new StreamHandler(contentLengthThread.getLooper());


        mResultHandler = new Handler(Looper.getMainLooper());
    }

    public PlayerCallback getDataForItem(StreamItem item, Range range) throws IOException {
        Log.d(getClass().getSimpleName(), "Get Data for item " + item.toString() + " " + range);

        Range chunkRange = range.chunkRange(chunkSize);
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
        mCurrentPosition = range.location;

        PlayerCallback pc = new PlayerCallback(item, range);

        if (missingChunksForRange.size() > 0) {
            mPlayerCallbacks.add(new PlayerCallback(item, range));
            addItem(item, missingChunksForRange, mHighPriorityQueue);
            updateLowPriorityQueue();
            processQueues();
        } else {
            Log.d(getClass().getSimpleName(), "Serving item from storage");
            pc.setByteBuffer(fetchStoredDataForItem(item, range));
        }
        return pc;
    }


    public void storeData(byte[] data, int chunk, StreamItem item) {
        Log.d(getClass().getSimpleName(), "Storing " + data.length + " bytes at index " + chunk + " for item " + item) ;
        mStorage.setData(data, chunk, item);
        fulfillPlayerCallbacks();
    }


    public void cleanup() {
        mConnectivityListener.stopListening();
        mConnectivityListener.unregisterHandler(mConnHandler);
        mConnectivityListener = null;

    }


    private ByteBuffer fetchStoredDataForItem(StreamItem item, Range byteRange) {
        Range actualRange = byteRange;
        if (item.getContentLength() != 0){
            actualRange = byteRange.intersection(Range.from(0, (int) item.getContentLength()));
        }

        if (actualRange == null){
            Log.e(getClass().getSimpleName(), "Invalid range, outside content length. Requested range " + byteRange + " from item " + item);
            return null;
        }

        Range chunkRange = actualRange.chunkRange(chunkSize);

        byte[] data = new byte[chunkRange.length * chunkSize];
        final int end = chunkRange.location + chunkRange.length;
        int writeIndex = 0;
        for (int chunkIndex = chunkRange.location; chunkIndex < end; chunkIndex++){
            byte[] chunkData = mStorage.getChunkData(item, chunkIndex);
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

        if (actualRange.length < data.length){
            return ByteBuffer.wrap(data, actualRange.location - (chunkRange.location * chunkSize), actualRange.length).slice().asReadOnlyBuffer();
        } else {
            return ByteBuffer.wrap(data);
        }
    }

    private void processQueues() {

        if (mHighPriorityConnection != null){
            if (!mHighPriorityConnection.executed) return;

            mHighPriorityConnection = null;
        }

        if (!isOnline()) return;

        if (mItemsNeedingHeadRequests.size() > 0){
            for (StreamItem item : mItemsNeedingHeadRequests){
                // start head connection for item
            }
            mItemsNeedingHeadRequests.clear();
        }

         if (mItemsNeedingPlayCountRequests.size() > 0){
            for (StreamItem item : mItemsNeedingPlayCountRequests){
                // start play count connection for item
            }
            mItemsNeedingPlayCountRequests.clear();
        }

        processHighPriorityQueue();
        if (mHighPriorityConnection == null){
            processLowPriorityQueue();
        }
    }

    private void processHighPriorityQueue() {

        if (mHighPriorityQueue.size() == 0) return;

        //Look if it is the current LowPriority Chunk.
        if (mLowPriorityConnection) {
            LoadingItem loadingItem = mHighPriorityQueue.get(0);
            List<Integer> indexes = loadingItem.indexes;


            /*
             TODO cancel low priority connection and re-add it to the queue
             https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L789
              */

        }

        for (LoadingItem highPriorityItem : mHighPriorityQueue) {
            StreamItem item = highPriorityItem.scStreamItem;
            if (!item.enabled) {
                mHighPriorityQueue.remove(highPriorityItem);
            } else if (item.getContentLength() != 0) {
                Range chunkRange = Range.from((Integer) highPriorityItem.indexes.get(0), 1);
                mHighPriorityConnection = startDataTask(item,chunkRange);
                        /*
                  highPriorityConnection = [[self startDataConnectionForItem:item range:chunkRange] retain];
            if (highPriorityConnection) {
                //If we have a connection, remove the chunk - it is taken care of and will be re-added in case of failure.
                [self removeItem:item chunks:[NSIndexSet indexSetWithIndexesInRange:chunkRange] fromQueue:highPriorityQueue];
                [self removeItem:item chunks:[NSIndexSet indexSetWithIndexesInRange:chunkRange] fromQueue:lowPriorityQueue];

            }   */
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

        for (LoadingItem lowPriorityItem : mHighPriorityQueue) {
            StreamItem item = lowPriorityItem.scStreamItem;
            if (!item.enabled) {
                mHighPriorityQueue.remove(lowPriorityItem);
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

    private void addItem(StreamItem item, Set<Integer> chunks, List<LoadingItem> queue) {
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

    private void removeItem(StreamItem item, Set<Long> chunks, List<LoadingItem> queue) {
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

    private void countPlayForItem(StreamItem item) {
        if (item == null) return;
        /*
        TODO request necessary range for a playcount
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L924
         */
    }



    private void fulfillPlayerCallbacks() {
        List<PlayerCallback> fulfilledCallbacks = new ArrayList<PlayerCallback>();
        for (PlayerCallback playerCallback : mPlayerCallbacks) {
            StreamItem item = playerCallback.scStreamItem;
            Range chunkRange = playerCallback.byteRange.chunkRange(chunkSize);
            Set<Integer> missingIndexes = mStorage.getMissingChunksForItem(item, chunkRange);
            if (missingIndexes.size() == 0) {
                fulfilledCallbacks.add(playerCallback);
            }
        }

        for (PlayerCallback playerCallback : fulfilledCallbacks) {
            ByteBuffer storedData = fetchStoredDataForItem(playerCallback.scStreamItem,playerCallback.byteRange);
            if (storedData != null){
                playerCallback.setByteBuffer(storedData);
                mPlayerCallbacks.remove(playerCallback);
            }
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

    private DataTask startDataTask(StreamItem item, Range chunkRange){
        DataTask dt = new DataTask(item, Range.from(chunkRange.location * chunkSize, chunkRange.length * chunkSize));
        Message msg = mHeadHandler.obtainMessage(item.url.hashCode(),dt);
        mDataHandler.sendMessage(msg);
        return dt;
    }

    private HeadTask startHeadTask(StreamItem item){
        if (!item.enabled) {
            Log.i(getClass().getSimpleName(), String.format("Can't start head for %s: Item is disabled." , item));
            return null;
        }
        if (!isOnline()) {
            mItemsNeedingHeadRequests.add(item);
            return null;
        }

        for (HeadTask ht : mHeadTasks){
            if (ht.getItem().equals(item)){
                return null;
            }
        }

        HeadTask ht = new HeadTask(item);

        Message msg = mHeadHandler.obtainMessage(item.url.hashCode(),ht);
        mHeadHandler.sendMessage(msg);

        return ht;
    }


    // request pipeline
    private class StreamHandler extends Handler {
        public StreamHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            DataTask task = (DataTask) msg.obj;
            if (task.execute()) {
                mResultHandler.post(task);
            }
        }
    }

    private static boolean isWaiting(Handler handler) {
        Looper looper = handler.getLooper();
        Thread thread = looper.getThread();
        Thread.State state = getThreadState(thread);
        return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
    }

    private static Thread.State getThreadState(Thread t) {
        try {
            return t.getState();
        } catch (ArrayIndexOutOfBoundsException e) {
            // Android 2.2.x seems to throw this exception occasionally
            Log.w(StreamLoader.class.getSimpleName(), e);
            return Thread.State.WAITING;
        }
    }

    private static class LoadingItem {
        StreamItem scStreamItem;
        List indexes;

        public LoadingItem(StreamItem item) {
            this.scStreamItem = item;
        }

        public LoadingItem(StreamItem scStreamItem, List indexes) {
            this(scStreamItem);
            this.indexes = indexes;
        }

        public int getWhat() {
            return scStreamItem.url.hashCode();
        }
    }
}