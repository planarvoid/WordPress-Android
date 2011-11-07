package com.soundcloud.android.streaming;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StreamLoader {
    static final String LOG_TAG = StreamLoader.class.getSimpleName();

    static final int CONNECTIVITY_MSG = 0;
    static final int MAX_RETRIES = 3;

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

    private final Set<StreamItem> mHeadTasks = Collections.synchronizedSet(new HashSet<StreamItem>());

    private final StreamHandler mDataHandler;
    private final StreamHandler mHeadHandler;
    private final Handler mResultHandler;
    private final Handler mConnHandler;
    private final Handler mPlaycountHandler;

    private boolean mForceOnline; /* for testing */

    static final int LOW_PRIO = 0;
    static final int HI_PRIO = 1;

    public StreamLoader(SoundCloudApplication context, final StreamStorage storage) {
        mContext = context;
        mStorage = storage;

        HandlerThread resultThread = new HandlerThread("streaming-result");
        resultThread.start();

        final Looper resultLooper = resultThread.getLooper();
        mResultHandler = new Handler(resultLooper) {
            @Override
            public void handleMessage(Message msg) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "result of message:" + msg.obj);

                if (msg.obj instanceof HeadTask) {
                    HeadTask t = (HeadTask) msg.obj;
                    if (t.item.isAvailable()) {
                        storage.storeMetadata(t.item);
                    }
                    mHeadTasks.remove(t.item);
                } else if (msg.obj instanceof DataTask) {
                    DataTask t = (DataTask) msg.obj;
                    if (t.item.isAvailable() && t.item.isRedirectValid()) {
                        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                            Log.d(LOG_TAG, String.format("Storing %d bytes at index %d for url %s",
                                t.buffer.limit(), t.chunkRange.start, t.item.url));
                        try {
                            if (!mStorage.storeData(t.item.url.toString(), t.buffer, t.chunkRange.start)) {
                                // try to fulfill callbacks directly if we couldn't store data
                                // (maybe SD storage was not available)
                                for (Iterator<StreamFuture> it = mPlayerCallbacks.iterator(); it.hasNext(); ) {
                                    StreamFuture cb = it.next();
                                    if (cb.item.equals(t.item) && cb.byteRange.equals(t.byteRange)) {
                                        cb.setByteBuffer(t.buffer);
                                        it.remove();
                                    }
                                }
                            }
                            fulfillPlayerCallbacks();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "error storing data", e);
                        }
                    }
                }
                processQueues();
            }
        };

        // setup connectivity listening
        mConnHandler = new Handler(resultLooper) {
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

                            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                                Log.d(LOG_TAG, "reconnected, processing queues");
                            processQueues();
                            break;
                        }
                }
            }
        };
        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);

        HandlerThread dataThread = new HandlerThread("streaming-data", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        dataThread.start();

        mDataHandler = new StreamHandler(context, dataThread.getLooper(), mResultHandler, MAX_RETRIES);

        HandlerThread headThread = new HandlerThread("streaming-head", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        headThread.start();

        mHeadHandler = new StreamHandler(context, headThread.getLooper(), mResultHandler, MAX_RETRIES);

        mPlaycountHandler = new Handler(resultLooper) {
            @Override
            public void handleMessage(Message msg) {
                String url = msg.obj.toString();
                mItemsNeedingPlaycountRequests.add(mStorage.getMetadata(url));
                processQueues();
            }
        };
    }

    public StreamFuture getDataForUrl(URL url, Range range) throws IOException {
        return getDataForUrl(url.toString(), range);
    }

    public StreamFuture getDataForUrl(String url, Range range) throws IOException {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, "Get data for url " + url + " " + range);

        final StreamItem item = mStorage.getMetadata(url);

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
            mResultHandler.post(new Runnable() {
                @Override public void run() {
                    mPlayerCallbacks.add(pc);
                    mHighPriorityQ.addItem(item, missingChunks);
                    updateLowPriorityQueue();
                    processQueues();
                }
            });
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                Log.d(LOG_TAG, "Serving item from storage");
            pc.setByteBuffer(mStorage.fetchStoredDataForUrl(item.url.toString(), range));
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
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "still hi-prio tasks, skip processing of lo-prio queue");
            } else {
                processLowPriorityQueue();
            }
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                Log.d(LOG_TAG, "not connected, skip processing of queues");
        }
    }

    private void processHighPriorityQueue() {
        for (StreamItem item : mItemsNeedingHeadRequests) {
            mItemsNeedingHeadRequests.remove(item);
            startHeadTask(item, HI_PRIO);
        }
        processItemQueue(mHighPriorityQ, HI_PRIO);
    }


    private void processLowPriorityQueue() {
        processItemQueue(mLowPriorityQueue, LOW_PRIO);

        for (StreamItem item : mItemsNeedingPlaycountRequests) {
            mItemsNeedingPlaycountRequests.remove(item);
            startPlaycountTask(item, LOW_PRIO);
        }
    }

    private void processItemQueue(ItemQueue q, int prio) {
        for (StreamItem item : q) {
            if (!item.isAvailable()) q.remove(item);
            //If there is a valid redirect for the item, download first chunk
            else if (item.isRedirectValid()) {
                Range chunkRange = Range.from(item.missingChunks.first(), 1);
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

            Index missingIndexes = mStorage.getMissingChunksForItem(item.url.toString(), chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(future);
            } else {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "still missing indexes, not fullfilling callback");
            }
        }

        for (StreamFuture playerCallback : fulfilledCallbacks) {
            try {
                playerCallback.setByteBuffer(mStorage.fetchStoredDataForUrl(playerCallback.item.url.toString(), playerCallback.byteRange));
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


    private DataTask startDataTask(StreamItem item, Range chunkRange, int prio) {
        DataTask task = DataTask.create(item, chunkRange, chunkRange.byteRange(mStorage.chunkSize), mContext);
        Message msg = mDataHandler.obtainMessage(prio, task);
        mDataHandler.sendMessage(msg);
        return task;
    }

    private PlaycountTask startPlaycountTask(StreamItem item, int prio) {
        PlaycountTask task = new PlaycountTask(item, mContext);
        mHeadHandler.sendMessage(mHeadHandler.obtainMessage(prio, task));
        return task;
    }

    private HeadTask startHeadTask(StreamItem item, int prio) {
        if (item.isAvailable()) {
            if (isConnected()) {
                synchronized (mHeadTasks) {
                    if (!mHeadTasks.contains(item)) {
                        mHeadTasks.add(item);
                        HeadTask ht = new HeadTask(item, mContext);
                        Message msg = mHeadHandler.obtainMessage(prio, ht);
                        mHeadHandler.sendMessage(msg);
                        return ht;
                    } else {
                        return null;
                    }
                }
            } else {
                mItemsNeedingHeadRequests.add(item);
                return null;
            }
        } else {
            Log.w(LOG_TAG, String.format("Can't start head for %s: Item is unavailable.", item));
            return null;
        }
    }

    /* package */ void setForceOnline(boolean b) {
        mForceOnline = b;
    }
}