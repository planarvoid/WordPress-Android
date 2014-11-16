package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.utils.BatteryListener;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
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

    private static final int LOW_PRIO = 0;
    private static final int HI_PRIO = 1;

    private final NetworkConnectivityListener connectivityListener;
    private final NetworkConnectionHelper connectivityHelper;
    private final BatteryListener batteryListener;
    private final Context context;
    private final StreamStorage storage;

    private final ItemQueue itemsNeedingHeadRequests = new ItemQueue();

    private StreamItem currentItem;

    private final Set<StreamFuture> playerCallbacks = new HashSet<StreamFuture>();
    private final ItemQueue highPriorityQueue = new ItemQueue();
    private final ItemQueue lowPriorityQueue = new ItemQueue();

    private final Set<StreamItem> headTasks = Collections.synchronizedSet(new HashSet<StreamItem>());

    private final StreamHandler dataHandler;
    private final StreamHandler headHandler;
    private final Handler resultHandler;
    private final Handler connHandler;

    private final HandlerThread dataThread;
    private final HandlerThread resultThread;
    private final HandlerThread headThread;

    private boolean forceOnline; /* for testing */

    private final PublicCloudAPI oldCloudAPI;

    public StreamLoader(Context context, final StreamStorage storage) {
        this.context = context;
        this.storage = storage;
        oldCloudAPI = new PublicApi(this.context);
        resultThread = new HandlerThread("streaming-result");
        resultThread.start();

        final Looper resultLooper = resultThread.getLooper();
        resultHandler = new ResultHandler(this, resultLooper);

        // setup connectivity listening
        connHandler = new ConnectivityHandler(this, resultLooper);

        connectivityListener = new NetworkConnectivityListener()
                .registerHandler(connHandler, CONNECTIVITY_MSG)
                .startListening(context);
        connectivityHelper = new NetworkConnectionHelper();
        batteryListener = new BatteryListener(context);

        dataThread = new HandlerThread("streaming-data", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        dataThread.start();

        dataHandler = new StreamHandler(context, dataThread.getLooper(), resultHandler, MAX_RETRIES);

        headThread = new HandlerThread("streaming-head", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        headThread.start();

        headHandler = new StreamHandler(context, headThread.getLooper(), resultHandler, MAX_RETRIES);
    }

    public StreamFuture getDataForUrl(URL url, Range range) throws IOException {
        return getDataForUrl(url.toString(), range);
    }

    public void preloadDataForUrl(final String url, final long delay) {
        // preload data if we have wifi and battery
        if (connectivityHelper.isWifiConnected() &&
                batteryListener.isOK() &&
                IOUtils.isSDCardAvailable()) {
            // cancel previous pending preload requests
            resultHandler.removeCallbacksAndMessages(PRELOAD_TOKEN);
            resultHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    final StreamItem item = storage.getMetadata(url);
                    // request first 3 chunks for next item
                    Index missing = storage.getMissingChunksForItem(url, Range.from(0, 3));
                    if (!missing.isEmpty()) {
                        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                            Log.d(LOG_TAG, "Connected to wifi, preloading data for url " + url);
                        }

                        lowPriorityQueue.addItem(item, missing);
                    }
                }
            }, PRELOAD_TOKEN, SystemClock.uptimeMillis() + delay);
        }
    }

    public StreamFuture getDataForUrl(String url, Range range) throws IOException {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "Get data for url " + url + " " + range);
        }

        final StreamItem item = storage.getMetadata(url);

        // no point trying if item is no longer available
        if (!item.isAvailable()) {
            throw new IOException("Item is not available");
        }

        final Index missing = storage.getMissingChunksForItem(url, range.chunkRange(storage.chunkSize));
        final StreamFuture pc = new StreamFuture(item, range);
        if (!missing.isEmpty()) {
            resultHandler.post(new Runnable() {
                @Override
                public void run() {
                    playerCallbacks.add(pc);
                    if (lowPriorityQueue.contains(item)) {
                        lowPriorityQueue.remove(item);
                    }

                    if (!item.equals(currentItem)) {
                        currentItem = item;
                        // remove low prio messages from handler
                        dataHandler.removeMessages(LOW_PRIO);
                    }
                    highPriorityQueue.addItem(item, missing);
                    processQueues();
                }
            });
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Serving item from storage");
            }
            pc.setByteBuffer(storage.fetchStoredDataForUrl(url, range));
        }
        return pc;
    }

    public void stop() {
        connectivityListener.stopListening();
        connectivityListener.unregisterHandler(connHandler);
        batteryListener.stopListening();

        headThread.quit();
        dataThread.quit();
        resultThread.quit();
    }

    private void processQueues() {
        if (isConnected()) {
            processHighPriorityQueue();

            if (headHandler.hasMessages(HI_PRIO) ||
                    dataHandler.hasMessages(HI_PRIO)) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "still hi-prio tasks, skip processing of lo-prio queue");
                }
            } else {
                processLowPriorityQueue();
            }
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "not connected, skip processing of queues");
            }
        }
    }

    private void processHighPriorityQueue() {
        for (StreamItem item : itemsNeedingHeadRequests) {
            itemsNeedingHeadRequests.remove(item);
            startHeadTask(item, HI_PRIO);
        }

        processItemQueue(highPriorityQueue, HI_PRIO);
    }


    private void processLowPriorityQueue() {
        processItemQueue(lowPriorityQueue, LOW_PRIO);
    }

    private void processItemQueue(ItemQueue q, int prio) {
        for (StreamItem item : q) {
            if (!item.isAvailable()) {
                q.remove(item);
            }
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
        for (StreamFuture future : playerCallbacks) {
            StreamItem item = future.item;
            Range chunkRange = future.byteRange.chunkRange(storage.chunkSize);

            Index missingIndexes = storage.getMissingChunksForItem(item.streamItemUrl(), chunkRange);
            if (missingIndexes.isEmpty()) {
                fulfilledCallbacks.add(future);
            } else {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "still missing indexes, not fullfilling callback");
                }
            }
        }

        for (StreamFuture sf : fulfilledCallbacks) {
            try {
                sf.setByteBuffer(storage.fetchStoredDataForUrl(sf.item.streamItemUrl(), sf.byteRange));
                playerCallbacks.remove(sf);
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
            }
        }
    }

    private boolean isConnected() {
        return forceOnline || connectivityHelper.networkIsConnected();
    }

    private DataTask startDataTask(StreamItem item, Range chunkRange, int prio) {
        final Range byteRange = chunkRange.byteRange(storage.chunkSize);
        if (item.getContentLength() > 0 && byteRange.start > item.getContentLength()) {
            // this can happen during prefetching
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, String.format("requested byterange %d > contentlength %d, not queuing task",
                        byteRange.start, item.getContentLength()));
            }

            return null;
        } else {
            final DataTask task = DataTask.create(item, chunkRange, byteRange, context);
            Message msg = dataHandler.obtainMessage(prio, task);
            if (prio == HI_PRIO) {
                dataHandler.sendMessageAtFrontOfQueue(msg);
            } else {
                dataHandler.sendMessage(msg);
            }
            return task;
        }
    }

    private HeadTask startHeadTask(StreamItem item, int prio) {
        if (item.isAvailable()) {
            if (isConnected()) {
                synchronized (headTasks) {
                    if (!headTasks.contains(item)) {
                        headTasks.add(item);
                        HeadTask ht = new HeadTask(item, oldCloudAPI, true);
                        Message msg = headHandler.obtainMessage(prio, ht);
                        headHandler.sendMessage(msg);
                        return ht;
                    } else {
                        return null;
                    }
                }
            } else {
                itemsNeedingHeadRequests.add(item);
                return null;
            }
        } else {
            Log.w(LOG_TAG, String.format("Can't start head for %s: Item is unavailable.", item));
            return null;
        }
    }

    /* package */ void setForceOnline(boolean b) {
        forceOnline = b;
    }

    private static final class ResultHandler extends Handler {

        private final WeakReference<StreamLoader> mLoaderRef;

        ResultHandler(StreamLoader loader, Looper looper) {
            super(looper);
            this.mLoaderRef = new WeakReference<>(loader);
        }

        @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void handleMessage(Message msg) {
            final StreamLoader loader = mLoaderRef.get();
            if (loader == null) {
                return;
            }

            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "result of message:" + msg.obj);
            }

            if (msg.obj instanceof HeadTask) {
                HeadTask t = (HeadTask) msg.obj;
                loader.headTasks.remove(t.item);
                if (t.item.isAvailable()) {
                    loader.storage.storeMetadata(t.item);
                } else {
                    // item not available, cancel futures
                    if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                        Log.d(LOG_TAG, "canceling load of item " + t.item);
                    }

                    for (StreamFuture f : new ArrayList<StreamFuture>(loader.playerCallbacks)) {
                        if (f.item.equals(t.item)) {
                            if (f.cancel(true)) {
                                loader.playerCallbacks.remove(f);
                            }
                        }
                    }
                }
            } else if (msg.obj instanceof DataTask) {
                DataTask t = (DataTask) msg.obj;
                if (msg.peekData() == null || !msg.getData().getBoolean(DataTask.SUCCESS_KEY)) {
                    // some failure, re-add item to queue, will be retried next time
                    loader.highPriorityQueue.addItem(t.item, t.chunkRange.toIndex());
                } else {
                    // for responsiveness, try to fulfill callbacks directly before storing buffer
                    for (Iterator<StreamFuture> it = loader.playerCallbacks.iterator(); it.hasNext(); ) {
                        StreamFuture cb = it.next();
                        if (cb.item.equals(t.item) && cb.byteRange.equals(t.byteRange)) {
                            cb.setByteBuffer(t.buffer.asReadOnlyBuffer());
                            it.remove();
                        }
                    }
                    try {
                        loader.storage.storeData(t.item.streamItemUrl(), t.buffer, t.chunkRange.start);
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
        private final WeakReference<StreamLoader> loaderRef;

        ConnectivityHandler(StreamLoader loader, Looper looper) {
            super(looper);
            this.loaderRef = new WeakReference<>(loader);
        }

        @Override
        public void handleMessage(Message msg) {
            final StreamLoader loader = loaderRef.get();
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
}
