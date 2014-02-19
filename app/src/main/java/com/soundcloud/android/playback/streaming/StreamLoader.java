package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.utils.BatteryListener;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
    static final Object PRELOAD_TOKEN = new Object();

    private final NetworkConnectivityListener mConnectivityListener;
    private final BatteryListener mBatteryListener;
    private final Context mContext;
    private final StreamStorage mStorage;

    private final ItemQueue mItemsNeedingHeadRequests = new ItemQueue();
    private final ItemQueue mItemsNeedingPlaycountRequests = new ItemQueue();

    private StreamItem mCurrentItem;

    private final Set<StreamFuture> mPlayerCallbacks = new HashSet<StreamFuture>();
    private final ItemQueue mHighPriorityQ = new ItemQueue();
    private final ItemQueue mLowPriorityQueue = new ItemQueue();

    private final Set<StreamItem> mHeadTasks = Collections.synchronizedSet(new HashSet<StreamItem>());

    private final StreamHandler mDataHandler;
    private final StreamHandler mHeadHandler;
    private final Handler mResultHandler;
    private final Handler mConnHandler;
    private final Handler mPlaycountHandler;

    private HandlerThread mDataThread;
    private HandlerThread mResultThread;
    private HandlerThread mHeadThread;

    private boolean mForceOnline; /* for testing */

    static final int LOW_PRIO = 0;
    static final int HI_PRIO = 1;
    private PublicCloudAPI mOldCloudAPI;
    private UMGCacheBuster umgCacheBuster;

    public StreamLoader(Context context, final StreamStorage storage) {
        mContext = context;
        mStorage = storage;
        mOldCloudAPI = new PublicApi(mContext);
        umgCacheBuster = new UMGCacheBuster(storage);
        mResultThread = new HandlerThread("streaming-result");
        mResultThread.start();

        final Looper resultLooper = mResultThread.getLooper();
        mResultHandler = new ResultHandler(this, resultLooper, umgCacheBuster);

        // setup connectivity listening
        mConnHandler = new ConnectivityHandler(this, resultLooper);

        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);

        mBatteryListener = new BatteryListener(context);

        mDataThread = new HandlerThread("streaming-data", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mDataThread.start();

        mDataHandler = new StreamHandler(context, mDataThread.getLooper(), mResultHandler, MAX_RETRIES);

        mHeadThread = new HandlerThread("streaming-head", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHeadThread.start();

        mHeadHandler = new StreamHandler(context, mHeadThread.getLooper(), mResultHandler, MAX_RETRIES);

        mPlaycountHandler = new PlaycountHandler(this, resultLooper);
    }

    public StreamFuture getDataForUrl(URL url, Range range) throws IOException {
        return getDataForUrl(url.toString(), range);
    }

    public void preloadDataForUrl(final String url, final long delay) {
        // preload data if we have wifi and battery
         if (mConnectivityListener.isWifiConnected() &&
                 mBatteryListener.isOK() &&
                 IOUtils.isSDCardAvailable()) {
            // cancel previous pending preload requests
            mResultHandler.removeCallbacksAndMessages(PRELOAD_TOKEN);
            mResultHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    final StreamItem item = mStorage.getMetadata(url);
                    // request first 3 chunks for next item
                    Index missing = mStorage.getMissingChunksForItem(url, Range.from(0, 3));
                    if (!missing.isEmpty()) {
                        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                            Log.d(LOG_TAG, "Connected to wifi, preloading data for url " + url);

                        mLowPriorityQueue.addItem(item, missing);
                    }
                }
            }, PRELOAD_TOKEN, SystemClock.uptimeMillis()+delay);
        }
    }

    public StreamFuture getDataForUrl(String url, Range range) throws IOException {
         umgCacheBuster.bustIt(url);

        final StreamItem item = mStorage.getMetadata(url);

        // no point trying if item is no longer available
        if (!item.isAvailable()) throw new IOException("Item is not available");

        final Index missing = mStorage.getMissingChunksForItem(url, range.chunkRange(mStorage.chunkSize));
        final StreamFuture pc = new StreamFuture(item, range);
        if (!missing.isEmpty()) {
            mResultHandler.post(new Runnable() {
                @Override public void run() {
                    mPlayerCallbacks.add(pc);
                    if (mLowPriorityQueue.contains(item)) mLowPriorityQueue.remove(item);

                    if (!item.equals(mCurrentItem)) {
                        // always request playcounts when switching tracks
                        mItemsNeedingPlaycountRequests.add(item);

                        mCurrentItem = item;
                        // remove low prio messages from handler
                        mDataHandler.removeMessages(LOW_PRIO);
                    }
                    mHighPriorityQ.addItem(item, missing);
                    processQueues();
                }
            });
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                Log.d(LOG_TAG, "Serving item from storage");
            pc.setByteBuffer(mStorage.fetchStoredDataForUrl(url, range));
        }
        return pc;
    }

    public boolean logPlaycount(String url) {
        return mPlaycountHandler.sendMessage(mPlaycountHandler.obtainMessage(LOW_PRIO, url));
    }

    public void stop() {
        mConnectivityListener.stopListening();
        mConnectivityListener.unregisterHandler(mConnHandler);
        mBatteryListener.stopListening();

        mHeadThread.quit();
        mDataThread.quit();
        mResultThread.quit();
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

                if (!item.missingChunks.isEmpty()) {
                    Range chunkRange = Range.from(item.missingChunks.first(), 1);
                    q.removeIfCompleted(item, chunkRange.toIndex());
                    startDataTask(item, chunkRange, prio);
                } else {
                    Log.d(LOG_TAG, "already downloaded all chunks");
                    q.remove(item);
                }
            } else {
                startHeadTask(item, prio);
            }
        }
    }

    private void fulfillPlayerCallbacks() {
        List<StreamFuture> fulfilledCallbacks = new ArrayList<StreamFuture>();
        for (StreamFuture future : mPlayerCallbacks) {
            StreamItem item = future.item;
            Range chunkRange = future.byteRange.chunkRange(mStorage.chunkSize);

            Index missingIndexes = mStorage.getMissingChunksForItem(item.streamItemUrl(), chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(future);
            } else {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "still missing indexes, not fullfilling callback");
            }
        }

        for (StreamFuture sf : fulfilledCallbacks) {
            try {
                sf.setByteBuffer(mStorage.fetchStoredDataForUrl(sf.item.streamItemUrl(), sf.byteRange));
                mPlayerCallbacks.remove(sf);
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
            }
        }
    }

    private boolean isConnected() {
        return mForceOnline || (mConnectivityListener != null && mConnectivityListener.isConnected());
    }

    private DataTask startDataTask(StreamItem item, Range chunkRange, int prio) {
        final Range byteRange = chunkRange.byteRange(mStorage.chunkSize);
        if (item.getContentLength() > 0 && byteRange.start > item.getContentLength()) {
            // this can happen during prefetching
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                Log.d(LOG_TAG, String.format("requested byterange %d > contentlength %d, not queuing task",
                        byteRange.start, item.getContentLength()));

            return null;
        } else {
            final DataTask task = DataTask.create(item, chunkRange, byteRange, mContext);
            Message msg = mDataHandler.obtainMessage(prio, task);
            if (prio == HI_PRIO) {
                mDataHandler.sendMessageAtFrontOfQueue(msg);
            } else {
                mDataHandler.sendMessage(msg);
            }
            return task;
        }
    }

    private PlaycountTask startPlaycountTask(StreamItem item, int prio) {
        PlaycountTask task = new PlaycountTask(item, mOldCloudAPI, true);
        mHeadHandler.sendMessage(mHeadHandler.obtainMessage(prio, task));
        return task;
    }

    private HeadTask startHeadTask(StreamItem item, int prio) {
        if (item.isAvailable()) {
            if (isConnected()) {
                synchronized (mHeadTasks) {
                    if (!mHeadTasks.contains(item)) {
                        mHeadTasks.add(item);
                        HeadTask ht = new HeadTask(item, mOldCloudAPI, true);
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

    private static final class ResultHandler extends Handler {

        private final WeakReference<StreamLoader> mLoaderRef;
        private final UMGCacheBuster umgCacheBuster;
        private ResultHandler(StreamLoader loader, Looper looper, UMGCacheBuster umgCacheBuster) {
            super(looper);
            this.mLoaderRef = new WeakReference<StreamLoader>(loader);
            this.umgCacheBuster = umgCacheBuster;
        }

        @Override
        public void handleMessage(Message msg) {
            final StreamLoader loader = mLoaderRef.get();
            if (loader == null) {
                return;
            }

            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "result of message:" + msg.obj);
            }
            StreamItemTask task = (StreamItemTask)msg.obj;

            if(!umgCacheBuster.getLastUrl().equals(task.item.streamItemUrl())){
                return;
            }
            if (task instanceof HeadTask) {
                HeadTask t = (HeadTask)task;
                loader.mHeadTasks.remove(t.item);
                if (t.item.isAvailable()) {
                    loader.mStorage.storeMetadata(t.item);
                } else {
                    // item not available, cancel futures
                    if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                        Log.d(LOG_TAG, "canceling load of item "+t.item);
                    }

                    for (StreamFuture f : new ArrayList<StreamFuture>(loader.mPlayerCallbacks)) {
                        if (f.item.equals(t.item)) {
                            if (f.cancel(true)) {
                                loader.mPlayerCallbacks.remove(f);
                            }
                        }
                    }
                }
            } else if (task instanceof DataTask) {
                DataTask dataTask = (DataTask) task;
                if (msg.peekData() == null || !msg.getData().getBoolean(DataTask.SUCCESS_KEY)) {
                    // some failure, re-add item to queue, will be retried next time
                    loader.mHighPriorityQ.addItem(dataTask.item, dataTask.chunkRange.toIndex());
                } else {
                    // for responsiveness, try to fulfill callbacks directly before storing buffer
                    for (Iterator<StreamFuture> it = loader.mPlayerCallbacks.iterator(); it.hasNext(); ) {
                        StreamFuture cb = it.next();
                        if (cb.item.equals(dataTask.item) && cb.byteRange.equals(dataTask.byteRange)) {
                            cb.setByteBuffer(dataTask.buffer.asReadOnlyBuffer());
                            it.remove();
                        }
                    }
                    try {
                        loader.mStorage.storeData(dataTask.item.streamItemUrl(), dataTask.buffer, dataTask.chunkRange.start);
                        loader.fulfillPlayerCallbacks();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "exception storing data", e);
                    }
                }
            }
            loader.processQueues();
        }

    }

    private static final class ConnectivityHandler extends Handler {
        private WeakReference<StreamLoader> mLoaderRef;

        private ConnectivityHandler(StreamLoader loader, Looper looper) {
            super(looper);
            this.mLoaderRef = new WeakReference<StreamLoader>(loader);
        }

        @Override
        public void handleMessage(Message msg) {
            final StreamLoader loader = mLoaderRef.get();
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    NetworkConnectivityListener.State previous =
                            NetworkConnectivityListener.State.values()[msg.arg1];
                    NetworkConnectivityListener.State current =
                            NetworkConnectivityListener.State.values()[msg.arg2];

                    if (loader != null && current == NetworkConnectivityListener.State.CONNECTED &&
                            previous == NetworkConnectivityListener.State.NOT_CONNECTED) {

                        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                            Log.d(LOG_TAG, "reconnected, processing queues");
                        }
                        loader.processQueues();
                        break;
                    }
            }
        }
    }

    private static final class PlaycountHandler extends Handler {
        private WeakReference<StreamLoader> mLoaderRef;

        private PlaycountHandler(StreamLoader loader, Looper looper) {
            super(looper);
            this.mLoaderRef = new WeakReference<StreamLoader>(loader);
        }

        @Override
        public void handleMessage(Message msg) {
            final StreamLoader loader = mLoaderRef.get();
            if (loader != null) {
                String url = msg.obj.toString();
                loader.mItemsNeedingPlaycountRequests.add(loader.mStorage.getMetadata(url));
                loader.processQueues();
            }
        }
    }
}