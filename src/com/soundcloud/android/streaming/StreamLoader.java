package com.soundcloud.android.streaming;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamLoader {
    static final String LOG_TAG = StreamLoader.class.getSimpleName();
    protected static final int CONNECTIVITY_MSG = 0;

    private NetworkConnectivityListener mConnectivityListener;
    private SoundCloudApplication mContext;
    private StreamStorage mStorage;

    private ItemQueue mItemsNeedingHeadRequests = new ItemQueue();
    private ItemQueue mItemsNeedingPlayCountRequests = new ItemQueue();

    private StreamItem mCurrentItem;
    private int mCurrentPosition;

    private Set<StreamFuture> mPlayerCallbacks = new HashSet<StreamFuture>();
    private ItemQueue mHighPriorityQ = new ItemQueue();
    private ItemQueue mLowPriorityQueue = new ItemQueue();

    private Set<HeadTask> mHeadTasks = new HashSet<HeadTask>();

    private StreamHandler mDataHandler;
    private StreamHandler mHeadHandler;


    private Map<String, StreamItem> mStreamItems = new HashMap<String, StreamItem>();

    private boolean mForceOnline; /* for testing */

    static final int LOW_PRIO = 0;
    static final int HI_PRIO  = 1;

    public StreamLoader(SoundCloudApplication context, final StreamStorage storage) {
        mContext = context;
        mStorage = storage;

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
                    try {
                        storeData(t.buffer, t.chunkRange.start, t.item);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "error storing data", e);
                    }
                }

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

    public StreamFuture getDataForItem(String url, Range range) throws IOException {
        Log.d(LOG_TAG, "Get Data for item " + url + " " + range);

        final StreamItem item;
        if (mStreamItems.containsKey(url)) {
            item = mStreamItems.get(url);
        } else {
            item = new StreamItem(url);
            mStreamItems.put(url, item);
        }

        Index missingChunks = mStorage.getMissingChunksForItem(item, range.chunkRange(mStorage.chunkSize));

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


    public void stop() {
        mConnectivityListener.stopListening();
        mConnectivityListener.unregisterHandler(mConnHandler);
        mConnectivityListener = null;
    }

    /* package */ ItemQueue getHighPriorityQueue() {
        return mHighPriorityQ;
    }

    /* package */ ItemQueue getLowPriorityQueue() {
        return mLowPriorityQueue;
    }

    private ByteBuffer fetchStoredDataForItem(StreamItem item, Range range) throws IOException {
        Range actualRange = range;
        if (item.getContentLength() != 0) {
            actualRange = range.intersection(item.byteRange());
            if (actualRange == null) {
                throw new IOException("Invalid range, outside content length. Requested range " + range + " from item " + item);
            }
        }
        Range chunkRange = actualRange.chunkRange(mStorage.chunkSize);

        ByteBuffer data = ByteBuffer.allocate(chunkRange.length * mStorage.chunkSize);
        for (int chunkIndex : chunkRange) {
            data.put(mStorage.getChunkData(item, chunkIndex));
        }
        data.limit(actualRange.length);
        data.rewind();
        return data;
    }

    private void storeData(ByteBuffer data, int chunk, StreamItem item) throws IOException {
        Log.d(LOG_TAG, String.format("Storing %d bytes at index %d for item %s", data.limit(), chunk, item));
        mStorage.storeData(data, chunk, item);
        fulfillPlayerCallbacks();
    }

    private void processQueues() {
        if (mHeadHandler.hasMessages(HI_PRIO) || mDataHandler.hasMessages(HI_PRIO)) {
            Log.d(LOG_TAG, "still working hi-prio, skip processing of queues");
            return;
        }

        if (isConnected()) {
            for (StreamItem item : mItemsNeedingHeadRequests) {
                // also done from processHighPriorityQueue()
                mItemsNeedingHeadRequests.remove(item);
                startHeadTask(item, HI_PRIO);
            }

            for (StreamItem item : mItemsNeedingPlayCountRequests) {
                mItemsNeedingPlayCountRequests.remove(item);
                // start play count connection for item
            }

            processHighPriorityQueue();
            processLowPriorityQueue();
        } else {
            Log.d(LOG_TAG, "not connected, skip processing of queues");
        }
    }

    private void processHighPriorityQueue() {
        if (mHighPriorityQ.isEmpty()) return;

        //Look if it is the current LowPriority Chunk.
        if (false) {
//            StreamItem loadingItem = mLowPriorityTask.item;
//            Index indexes = loadingItem.index;
            /*
             TODO cancel low priority connection and re-add it to the hi-priority queue
             https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L789
            */
        }


        for (StreamItem item : mHighPriorityQ) {
            if (item.unavailable) mHighPriorityQ.remove(item);

            //If there is a contentLength for the item, download first chunk
            else if (item.getContentLength() > 0 && item.redirectedURL != null) {
                Range chunkRange = Range.from(item.index.first(), 1);

                if (mHighPriorityQ.removeIfCompleted(item, chunkRange.toIndex())) {
                    startDataTask(item, chunkRange, HI_PRIO);
                }

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

                startHeadTask(item, HI_PRIO);
            }

        }
    }

    private void processLowPriorityQueue() {
        for (StreamItem item : mLowPriorityQueue) {
            if (item.unavailable) {
                mLowPriorityQueue.remove(item);
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
            StreamItem item = playerCallback.item;
            Range chunkRange = playerCallback.byteRange.chunkRange(mStorage.chunkSize);

            Index missingIndexes = mStorage.getMissingChunksForItem(item, chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(playerCallback);
            } else {
                Log.d(LOG_TAG, "still missing indexes");
            }
        }

        for (StreamFuture playerCallback : fulfilledCallbacks) {
            try {
                playerCallback.setByteBuffer(fetchStoredDataForItem(playerCallback.item, playerCallback.byteRange));
                mPlayerCallbacks.remove(playerCallback);
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
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
//                    processQueues();
                    break;
            }
        }
    };

    private DataTask startDataTask(StreamItem item, Range chunkRange, int prio) {
        Log.d(LOG_TAG, "startDataTask("+item+")");

        DataTask task = new DataTask(item, chunkRange, chunkRange.byteRange(mStorage.chunkSize), mContext);
        Message msg = mDataHandler.obtainMessage(prio, task);

        mDataHandler.sendMessage(msg);
        return task;
    }


    private HeadTask startHeadTask(StreamItem item, int prio) {
        Log.d(LOG_TAG, "startHeadTask("+item+")");

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
        Message msg = mHeadHandler.obtainMessage(prio, ht);
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
                mHandler.sendMessage(obtainMessage(msg.what, msg.obj));
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
                if (msg.arg1 < 3) {
                    Log.d(LOG_TAG, "retrying, tries="+msg.arg1);
                    sendMessageDelayed(obtainMessage(msg.what,  msg.arg1+1, 0, msg.obj), 500);
                } else {
                    Log.d(LOG_TAG, "giving up");
                }
            }
        }
    }

    /* package */ void setForceOnline(boolean b) {
        mForceOnline = b;
    }
}