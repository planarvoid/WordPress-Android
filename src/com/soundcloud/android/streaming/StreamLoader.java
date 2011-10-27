package com.soundcloud.android.streaming;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    static final String LOG_TAG = StreamLoader.class.getSimpleName();
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

    private boolean mForceOnline; /* for testing */

    public StreamLoader(SoundCloudApplication context, final StreamStorage storage) {
        mContext = context;
        mStorage = storage;
        chunkSize = storage.chunkSize;

        // setup connectivity listening
        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);


        Handler resultHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(LOG_TAG, "result of message:" + msg.obj);
                if (msg.obj instanceof HeadTask) {
                    HeadTask t = (HeadTask) msg.obj;
                    mHeadTasks.remove(t);
                } else if (msg.obj instanceof DataTask) {
                    DataTask t = (DataTask) msg.obj;
                    storeData(t.buffer, t.chunkRange.start, t.item);
                }

                if (msg.obj == mHighPriorityTask) mHighPriorityTask = null;
                else if (msg.obj == mLowPriorityTask) mLowPriorityTask = null;

                processQueues();
            }
        };

        HandlerThread dataThread = new HandlerThread("streaming-data", THREAD_PRIORITY_BACKGROUND);
        dataThread.start();

        mDataHandler = new StreamHandler(dataThread.getLooper(), resultHandler);

        HandlerThread contentLengthThread = new HandlerThread("streaming-head", THREAD_PRIORITY_BACKGROUND);
        contentLengthThread.start();

        mHeadHandler = new StreamHandler(contentLengthThread.getLooper(), resultHandler);
    }

    public StreamFuture getDataForItem(StreamItem item, Range range) throws IOException {
        Log.d(LOG_TAG, "Get Data for item " + item.toString() + " " + range);

        Index missingChunks = mStorage.getMissingChunksForItem(item, range.chunkRange(chunkSize));

        //If the current item changes
        if (!item.equals(mCurrentItem)) {
            mItemsNeedingHeadRequests.add(item);

            // If we won't request the 0th byte
            // (by either having it already OR jumping into the middle of a new track)
            if (!missingChunks.get(0)) {
                countPlayForItem(item);
            }
        }

        mCurrentItem = item;
        mCurrentPosition = range.start;

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
        Log.d(LOG_TAG, String.format("Storing %d bytes at index %d for item %s", data.length, chunk, item));
        mStorage.setData(data, chunk, item);
        fulfillPlayerCallbacks();
    }

    public void stop() {
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
            actualRange = range.intersection(item.byteRange());
        }

        if (actualRange == null) {
            Log.e(LOG_TAG, "Invalid range, outside content length. Requested range " + range + " from item " + item);
            return null;
        }

        Range chunkRange = actualRange.chunkRange(chunkSize);

        byte[] data = new byte[chunkRange.length * chunkSize];
        final int end = chunkRange.start + chunkRange.length;
        int writeIndex = 0;
        for (int chunkIndex = chunkRange.start; chunkIndex < end; chunkIndex++) {
            byte[] chunkData = mStorage.getChunkData(item, chunkIndex);
            if (chunkData == null) {
                Log.e(LOG_TAG, "Error getting chunk data, aborting");
                return null;
            }
            System.arraycopy(chunkData, 0, data, writeIndex, chunkData.length);
            writeIndex += chunkData.length;
        }

        if (actualRange.length < data.length) {
            return ByteBuffer.wrap(data, actualRange.start - (chunkRange.start * chunkSize), actualRange.length).slice().asReadOnlyBuffer();
        } else {
            return ByteBuffer.wrap(data);
        }
    }

    private void processQueues() {
        if (mHighPriorityTask != null) {
            if (mHighPriorityTask.isExecuted()) {
                mHighPriorityTask = null;
            } else {
                Log.d(LOG_TAG, "still working hi-prio, skip processing of queues");
                return;
            }
        }

        if (isConnected()) {
            for (Iterator<StreamItem> it = mItemsNeedingHeadRequests.iterator(); it.hasNext();) {
                // also done from processHighPriorityQueue()
                final StreamItem item = it.next();
                it.remove();
                startHeadTask(item);
            }

            for (Iterator<StreamItem >it = mItemsNeedingPlayCountRequests.iterator();it.hasNext();) {
                it.remove();
                // start play count connection for item
            }

            processHighPriorityQueue();
            if (mHighPriorityTask == null) {
                processLowPriorityQueue();
            }
        } else {
            Log.d(LOG_TAG, "not connected, skip processing of queues");
        }
    }

    private void processHighPriorityQueue() {
        if (mHighPriorityQ.isEmpty()) return;

        //Look if it is the current LowPriority Chunk.
        if (mLowPriorityTask != null) {
            StreamItem loadingItem = mLowPriorityTask.item;
            Index indexes = loadingItem.index;
            /*
             TODO cancel low priority connection and re-add it to the hi-priority queue
             https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L789
            */
        }

        for(Iterator<StreamItem> it = mHighPriorityQ.iterator(); it.hasNext();) {
            final StreamItem item = it.next();
            if (item.unavailable) it.remove();
            //If there is a contentLength for the item, download first chunk
            else if (item.getContentLength() > 0 && item.redirectedURL != null) {
                Range chunkRange = Range.from(item.index.first(), 1);
                it.remove();
                item.index.andNot(chunkRange.toIndex());

                mHighPriorityTask = startDataTask(item, chunkRange);

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
            if (item.unavailable) {
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
        if (item != null)  {
            /*
            TODO request necessary range for a playcount
            migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L924
             */
        }
    }

    private void fulfillPlayerCallbacks() {
        List<StreamFuture> fulfilledCallbacks = new ArrayList<StreamFuture>();
        for (StreamFuture playerCallback : mPlayerCallbacks) {
            StreamItem item = playerCallback.streamItem;
            Range chunkRange = playerCallback.byteRange.chunkRange(chunkSize);
            Index missingIndexes = mStorage.getMissingChunksForItem(item, chunkRange);
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

    private boolean isConnected() {
        if (mForceOnline) {
            return true;
        } else {
            //return mConnectivityListener.isConnected();
            ConnectivityManager c = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = c.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    private Handler mConnHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    processQueues();
                    break;
            }
        }
    };

    private DataTask startDataTask(StreamItem item, Range chunkRange) {
        DataTask task = new DataTask(item, chunkRange, chunkRange.byteRange(chunkSize), mContext);
        Message msg = mDataHandler.obtainMessage(item.hashCode(), task);
        mDataHandler.sendMessage(msg);
        return task;
    }

    private HeadTask startHeadTask(StreamItem item) {
        if (item.unavailable) {
            Log.d(LOG_TAG, String.format("Can't start head for %s: Item is unavailable.", item));
            return null;
        } else if (!isConnected()) {
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
    static class StreamHandler extends Handler {
        private Handler mHandler;

        public StreamHandler(Looper looper, Handler resultHandler) {
            super(looper);
            mHandler = resultHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "StreamHandler: handle "+msg.obj);

            StreamItemTask task = (StreamItemTask) msg.obj;
            try {
                task.execute();
                mHandler.sendMessage(obtainMessage(task.item.hashCode(), msg.obj));
            } catch (IOException e) {
                //Log.w(LOG_TAG, e);
                throw new RuntimeException(e);
            }
        }
    }

    /* package */ void setForceOnline(boolean b) {
        mForceOnline = b;
    }
}