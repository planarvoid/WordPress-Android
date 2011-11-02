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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamLoader {
    static final String LOG_TAG = StreamLoader.class.getSimpleName();
    protected static final int CONNECTIVITY_MSG = 0;

    private NetworkConnectivityListener mConnectivityListener;
    private final SoundCloudApplication mContext;
    private final StreamStorage mStorage;

    private final ItemQueue mItemsNeedingHeadRequests = new ItemQueue();
    private final ItemQueue mItemsNeedingPlaycountRequests = new ItemQueue();

    private StreamItem mCurrentItem;
    private int mCurrentPosition;

    private final Set<StreamFuture> mPlayerCallbacks = new HashSet<StreamFuture>();
    private final ItemQueue mHighPriorityQ = new ItemQueue();
    private final ItemQueue mLowPriorityQueue = new ItemQueue();

    private final Set<HeadTask> mHeadTasks = new HashSet<HeadTask>();

    private StreamHandler mDataHandler;
    private StreamHandler mHeadHandler;
    private Handler mResultHandler;

    private boolean mForceOnline; /* for testing */

    static final int LOW_PRIO = 0;
    static final int HI_PRIO = 1;

    public StreamLoader(SoundCloudApplication context, final StreamStorage storage) {
        mContext = context;
        mStorage = storage;

        // setup connectivity listening
        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);


        mResultHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(LOG_TAG, "result of message:" + msg.obj);
                if (msg.obj instanceof HeadTask) {
                    HeadTask t = (HeadTask) msg.obj;
                    if (storage.storeMetadata(t.item)) {
                        mHeadTasks.remove(t);
                    }
                } else if (msg.obj instanceof DataTask) {
                    DataTask t = (DataTask) msg.obj;
                    try {
                        Log.d(LOG_TAG, String.format("Storing %d bytes at index %d for url %s",
                                t.buffer.limit(), t.chunkRange.start, t.item.url));
                        mStorage.storeData(t.item.url, t.buffer, t.chunkRange.start);
                        fulfillPlayerCallbacks();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "error storing data", e);
                    }
                } else if (msg.obj instanceof PlaycountTask) {

                }
                processQueues();
            }
        };

        HandlerThread dataThread = new HandlerThread("streaming-data", THREAD_PRIORITY_BACKGROUND);
        dataThread.start();

        mDataHandler = new StreamHandler(dataThread.getLooper(), mResultHandler);

        HandlerThread contentLengthThread = new HandlerThread("streaming-head", THREAD_PRIORITY_BACKGROUND);
        contentLengthThread.start();

        mHeadHandler = new StreamHandler(contentLengthThread.getLooper(), mResultHandler);
    }

    public StreamFuture getDataForUrl(String url, Range range) throws IOException {
        Log.d(LOG_TAG, "Get data for url " + url + " " + range);

        StreamItem item = mStorage.getMetadata(url);
        if (item == null ) {
            item = new StreamItem(url);
        }
        //If there is no metadata yet or it is expired, request it
        if (!item.isRedirectValid()) {
            mItemsNeedingHeadRequests.add(item);
        }

        final Index missingChunks = mStorage.getMissingChunksForItem(url, range.chunkRange(mStorage.chunkSize));
        if (!item.equals(mCurrentItem)) {
            // If we won't request the 0th byte
            // (by either having it already OR jumping into the middle of a new track)
            if (!missingChunks.get(0)) {
                mItemsNeedingPlaycountRequests.add(item);
            }
            mCurrentItem = item;
        }
        mCurrentPosition = range.start;

        final StreamFuture pc = new StreamFuture(item, range);
        if (!missingChunks.isEmpty()) {
            final StreamItem theItem = item;
            mResultHandler.post(new Runnable() {
                @Override public void run() {
                    mPlayerCallbacks.add(pc);
                    mHighPriorityQ.addItem(theItem, missingChunks);
                    updateLowPriorityQueue();
                    processQueues();
                }
            });
        } else {
            Log.d(LOG_TAG, "Serving item from storage");
            pc.setByteBuffer(mStorage.fetchStoredDataForUrl(item.url, range));
        }
        return pc;
    }

    public boolean logPlaycount(String url) {
        return mPlaycountHandler.sendMessage(mPlaycountHandler.obtainMessage(LOW_PRIO, url));
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

    private void processQueues() {
        if (isConnected()) {
            processHighPriorityQueue();

            if (mHeadHandler.hasMessages(HI_PRIO) ||
                mDataHandler.hasMessages(HI_PRIO)) {
                Log.d(LOG_TAG, "still hi-prio tasks, skip processing of lo-prio queue");
            } else {
                processLowPriorityQueue();
            }
        } else {
            Log.d(LOG_TAG, "not connected, skip processing of queues");
        }
    }

    private void processHighPriorityQueue() {
        for (StreamItem item : mItemsNeedingHeadRequests) {
            // also done from processHighPriorityQueue()
            mItemsNeedingHeadRequests.remove(item);
            startHeadTask(item, HI_PRIO);
        }
        processItemQueue(mHighPriorityQ, HI_PRIO);
    }


    private void processLowPriorityQueue() {
        processItemQueue(mLowPriorityQueue, LOW_PRIO);

        for (StreamItem item : mItemsNeedingPlaycountRequests) {
            if (item.isRedirectValid()) {
                mItemsNeedingPlaycountRequests.remove(item);
                startPlaycountTask(item, LOW_PRIO);
            }
        }
    }

    private void processItemQueue(ItemQueue q, int prio) {
        for (StreamItem item : q) {
            if (!item.isAvailable()) q.remove(item);
            //If there is a valid redirect for the item, download first chunk
            else if (item.isRedirectValid()) {
                final int firstChunk = item.chunksToDownload.first();
                if (firstChunk < 0) {
                    throw new AssertionError("firstChunk < 0 for "+item);
                }
                Range chunkRange = Range.from(firstChunk, 1);
                q.removeIfCompleted(item, chunkRange.toIndex());
                startDataTask(item, chunkRange, prio);
            } else {
                startHeadTask(item, prio);
            }
        }

    }

    private void updateLowPriorityQueue() {
        /*
        TODO prefetching. Not sure how much we want to do yet
        migrate : https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamLoader.m#L573
         */
    }

    private void fulfillPlayerCallbacks() {
        List<StreamFuture> fulfilledCallbacks = new ArrayList<StreamFuture>();
        for (StreamFuture future : mPlayerCallbacks) {
            StreamItem item = future.item;
            Range chunkRange = future.byteRange.chunkRange(mStorage.chunkSize);

            Index missingIndexes = mStorage.getMissingChunksForItem(item.url, chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(future);
            } else {
                Log.d(LOG_TAG, "still missing indexes, not fullfilling callback");
            }
        }

        for (StreamFuture playerCallback : fulfilledCallbacks) {
            try {
                playerCallback.setByteBuffer(mStorage.fetchStoredDataForUrl(playerCallback.item.url, playerCallback.byteRange));
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
                    NetworkConnectivityListener.State previous =
                            NetworkConnectivityListener.State.values()[msg.arg1];
                    NetworkConnectivityListener.State current =
                            NetworkConnectivityListener.State.values()[msg.arg2];

                    if (current == NetworkConnectivityListener.State.CONNECTED &&
                        previous == NetworkConnectivityListener.State.NOT_CONNECTED) {

                        Log.d(LOG_TAG, "reconnected, processing queues");
                        processQueues();
                        break;
                    }
            }
        }
    };

    private Handler mPlaycountHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String url = msg.obj.toString();
            StreamItem item = mStorage.getMetadata(url);
            if (item != null) {
                if (item.redirectUrl() == null || item.isRedirectExpired()) {
                    mItemsNeedingHeadRequests.add(item);
                }
                mItemsNeedingPlaycountRequests.add(item);
                processQueues();
            }
        }
    };

    private DataTask startDataTask(StreamItem item, Range chunkRange, int prio) {
        Log.d(LOG_TAG, "startDataTask(" + item + ", prio=" + prio + ")");

        DataTask task = new DataTask(item, chunkRange, chunkRange.byteRange(mStorage.chunkSize), mContext);
        Message msg = mDataHandler.obtainMessage(prio, task);

        mDataHandler.sendMessage(msg);
        return task;
    }

    private PlaycountTask startPlaycountTask(StreamItem item, int prio) {
        Log.d(LOG_TAG, "startPlaycountTask(" + item + ", prio=" + prio + ")");
        PlaycountTask task = new PlaycountTask(item, mContext);
        mHeadHandler.sendMessage(mHeadHandler.obtainMessage(prio, task));
        return task;
    }

    private HeadTask startHeadTask(StreamItem item, int prio) {
        Log.d(LOG_TAG, "startHeadTask(" + item + ",prio=" + prio + ")");
        if (item.isAvailable()) {
            if (isConnected()) {
                for (HeadTask ht : mHeadTasks) {
                    if (ht.item.equals(item)) {
                        return null;
                    }
                }
                HeadTask ht = new HeadTask(item, mContext);
                Message msg = mHeadHandler.obtainMessage(prio, ht);
                mHeadHandler.sendMessage(msg);

                return ht;
            } else {
                mItemsNeedingHeadRequests.add(item);
                return null;
            }
        } else {
            Log.d(LOG_TAG, String.format("Can't start head for %s: Item is unavailable.", item));
            return null;
        }
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
            Log.d(LOG_TAG, "StreamHandler: handle " + msg.obj);

            StreamItemTask task = (StreamItemTask) msg.obj;
            try {
                Message result = obtainMessage(msg.what, msg.obj);
                result.setData(task.execute());
                mHandler.sendMessage(result);
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
                if (task.item.isAvailable() && msg.arg1 < 3) {
                    Log.d(LOG_TAG, "retrying, tries=" + msg.arg1);
                    sendMessageDelayed(obtainMessage(msg.what, msg.arg1 + 1, 0, msg.obj), 200 * msg.arg1);
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