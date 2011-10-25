package com.soundcloud.android.streaming;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class StreamLoader {
    static final String LOG_TAG = HeadTask.class.getSimpleName();
    protected static final int CONNECTIVITY_MSG = 0;

    private NetworkConnectivityListener mConnectivityListener;
    private SoundCloudApplication mContext;
    private StreamStorage mStorage;
    private List<StreamItem> mItemsNeedingHeadRequests = new ArrayList<StreamItem>();
    private List<StreamItem> mItemsNeedingPlayCountRequests = new ArrayList<StreamItem>();

    private int chunkSize;
    private StreamItem mCurrentItem;
    private int mCurrentPosition;

    private Set<StreamFuture> mPlayerCallbacks = new HashSet<StreamFuture>();
    private LoadingQueue mHighPriorityQ = new LoadingQueue();
    private LoadingQueue mLowPriorityQueue = new LoadingQueue();

    private StreamItemTask mHighPriorityTask;
    private StreamItemTask mLowPriorityTask;

    private Set<HeadTask> mHeadTasks = new HashSet<HeadTask>();

    private StreamHandler mDataHandler;
    private StreamHandler mHeadHandler;

    private Handler mResultHandler;

    private boolean mForceOnline; /* for testing */

    public StreamLoader(SoundCloudApplication context, StreamStorage storage) {
        mContext = context;
        mStorage = storage;
        chunkSize = storage.chunkSize;

        // setup connectivity listening
        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);

        HandlerThread dataThread = new HandlerThread("streaming-data", THREAD_PRIORITY_BACKGROUND);
        dataThread.start();

        mDataHandler = new StreamHandler(dataThread.getLooper());

        HandlerThread contentLengthThread = new HandlerThread("streaming-head", THREAD_PRIORITY_BACKGROUND);
        contentLengthThread.start();

        mHeadHandler = new StreamHandler(contentLengthThread.getLooper());

        mResultHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(LOG_TAG, "handleMessage:" + msg.obj);
                if (msg.obj instanceof HeadTask) {
                    HeadTask t = (HeadTask) msg.obj;
                    mHeadTasks.remove(t);
                    mItemsNeedingHeadRequests.remove(t.item);
                } else if (msg.obj instanceof DataTask) {
                    DataTask t = (DataTask) msg.obj;
                }
                processQueues();
            }
        };
    }

    public StreamFuture getDataForItem(StreamItem item, Range range) throws IOException {
        Log.d(LOG_TAG, "Get Data for item " + item.toString() + " " + range);

        IndexSet missingChunks = mStorage.getMissingChunksForItem(item, range.chunkRange(chunkSize));

        //If the current item changes
        if (!item.equals(mCurrentItem)) {
            mItemsNeedingHeadRequests.add(item);

            // If we won't request the 0th byte (by either having it already OR jumping into the middle of a new track)
            if (!missingChunks.contains(0)) {
                countPlayForItem(item);
            }
        }

        mCurrentItem = item;
        mCurrentPosition = range.location;

        StreamFuture pc = new StreamFuture(item, range);

        if (!missingChunks.isEmpty()) {
            mPlayerCallbacks.add(pc);
            mHighPriorityQ.addItem(item, missingChunks);
            updateLowPriorityQueue();
            processQueues();
        } else {
            Log.d(LOG_TAG, "Serving item from storage");
            pc.setByteBuffer(fetchStoredDataForItem(item, range));
        }
        return pc;
    }


    public void storeData(byte[] data, int chunk, StreamItem item) {
        Log.d(LOG_TAG, "Storing " + data.length + " bytes at index " + chunk + " for item " + item);
        mStorage.setData(data, chunk, item);
        fulfillPlayerCallbacks();
    }


    public void cleanup() {
        mConnectivityListener.stopListening();
        mConnectivityListener.unregisterHandler(mConnHandler);
        mConnectivityListener = null;

    }


    /* package */ LoadingQueue getHighPriorityQueue() {
        return mHighPriorityQ;
    }

    /* package */ LoadingQueue getLowPriorityQueue() {
        return mLowPriorityQueue;
    }


    private ByteBuffer fetchStoredDataForItem(StreamItem item, Range range) {
        Range actualRange = range;
        if (item.getContentLength() != 0) {
            actualRange = range.intersection(item.getRange());
        }

        if (actualRange == null) {
            Log.e(LOG_TAG, "Invalid range, outside content length. Requested range " + range + " from item " + item);
            return null;
        }

        Range chunkRange = actualRange.chunkRange(chunkSize);

        byte[] data = new byte[chunkRange.length * chunkSize];
        final int end = chunkRange.location + chunkRange.length;
        int writeIndex = 0;
        for (int chunkIndex = chunkRange.location; chunkIndex < end; chunkIndex++) {
            byte[] chunkData = mStorage.getChunkData(item, chunkIndex);
            if (chunkData == null) {
                Log.e(LOG_TAG, "Error getting chunk data, aborting");
                return null;
            }
            int i = 0;
            while (i < chunkData.length) {
                data[writeIndex + i] = chunkData[i];
                i++;
            }
            writeIndex += i;
        }

        if (actualRange.length < data.length) {
            return ByteBuffer.wrap(data, actualRange.location - (chunkRange.location * chunkSize), actualRange.length).slice().asReadOnlyBuffer();
        } else {
            return ByteBuffer.wrap(data);
        }
    }

    private void processQueues() {
        if (mHighPriorityTask != null) {
            if (mHighPriorityTask.isExecuted()) {
                mHighPriorityTask = null;
            } else {
                // still working
                return;
            }
        }

        if (isOnline()) {
            if (!mItemsNeedingHeadRequests.isEmpty()) {
                for (StreamItem item : mItemsNeedingHeadRequests) {
                    // also done from processHighPriorityQueue()
                    startHeadTask(item);
                }
                mItemsNeedingHeadRequests.clear();
            }

            if (!mItemsNeedingPlayCountRequests.isEmpty()) {
                for (StreamItem item : mItemsNeedingPlayCountRequests) {
                    // start play count connection for item
                }
                mItemsNeedingPlayCountRequests.clear();
            }

            processHighPriorityQueue();
            if (mHighPriorityTask == null) {
                processLowPriorityQueue();
            }
        }
    }

    private void processHighPriorityQueue() {
        if (mHighPriorityQ.isEmpty()) return;

        //Look if it is the current LowPriority Chunk.
        if (mLowPriorityTask != null) {
            StreamItem loadingItem = mLowPriorityTask.item;
            IndexSet indexes = loadingItem.index;
            /*
             TODO cancel low priority connection and re-add it to the hi-priority queue
             https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L789
            */
        }

        for(Iterator<StreamItem> it = mHighPriorityQ.iterator(); it.hasNext();) {
            final StreamItem item = it.next();
            if (!item.available) {
                it.remove();
            }
            //If there is a contentLength for the item, download first chunk
            else if (item.getContentLength() > 0) {
                Range chunkRange = Range.from(item.index.first(), 1);
                mHighPriorityTask = startDataTask(item, chunkRange);

                //item.indexes.removeAll()
                it.remove();

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
                startHeadTask(item);
            }

        }
    }

    private void processLowPriorityQueue() {
        if (mLowPriorityTask != null || mLowPriorityQueue.isEmpty()) return;
        for (Iterator<StreamItem> it = mLowPriorityQueue.iterator(); it.hasNext();) {
            StreamItem item = it.next();
            if (!item.available) {
                it.remove();
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

    private void countPlayForItem(StreamItem item) {
        if (item == null) return;
        /*
        TODO request necessary range for a playcount
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L924
         */
    }


    private void fulfillPlayerCallbacks() {
        List<StreamFuture> fulfilledCallbacks = new ArrayList<StreamFuture>();
        for (StreamFuture playerCallback : mPlayerCallbacks) {
            StreamItem item = playerCallback.streamItem;
            Range chunkRange = playerCallback.byteRange.chunkRange(chunkSize);
            Set<Integer> missingIndexes = mStorage.getMissingChunksForItem(item, chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(playerCallback);
            }
        }

        for (StreamFuture playerCallback : fulfilledCallbacks) {
            ByteBuffer storedData = fetchStoredDataForItem(playerCallback.streamItem, playerCallback.byteRange);
            if (storedData != null) {
                playerCallback.setByteBuffer(storedData);
                mPlayerCallbacks.remove(playerCallback);
            }
        }
    }

    private boolean isOnline() {
        return mForceOnline || mConnectivityListener != null && mConnectivityListener.isConnected();
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

    private DataTask startDataTask(StreamItem item, Range chunkRange) {
        DataTask task = new DataTask(item, Range.from(chunkRange.location * chunkSize, chunkRange.length * chunkSize), mContext);
        Message msg = mDataHandler.obtainMessage(item.hashCode(), task);
        mDataHandler.sendMessage(msg);
        return task;
    }

    private HeadTask startHeadTask(StreamItem item) {
        if (!item.available) {
            Log.i(LOG_TAG, String.format("Can't start head for %s: Item is disabled.", item));
            return null;
        }
        if (!isOnline()) {
            mItemsNeedingHeadRequests.add(item);
            return null;
        }

        for (HeadTask ht : mHeadTasks) {
            if (ht.item.equals(item)) {
                return null;
            }
        }

        HeadTask ht = new HeadTask(item, mContext);
        Message msg = mHeadHandler.obtainMessage(item.hashCode(), ht);
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
            StreamItemTask task = (StreamItemTask) msg.obj;
            try {
                task.execute();
                mResultHandler.sendMessage(msg);
            } catch (IOException e) {
                //Log.w(LOG_TAG, e);
                throw new RuntimeException(e);
            }
        }
    }

    /* package */ void setForceOnline(boolean b) {
        mForceOnline = b;
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
            Log.w(LOG_TAG, e);
            return Thread.State.WAITING;
        }
    }
}