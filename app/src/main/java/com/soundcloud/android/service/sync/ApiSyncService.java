package com.soundcloud.android.service.sync;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.ParallelAsyncTask;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ApiSyncService extends Service {
    public static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    public static final String ACTION_APPEND    = "com.soundcloud.android.sync.action.APPEND";
    public static final String ACTION_PUSH      = "com.soundcloud.android.sync.action.PUSH";

    public static final String EXTRA_SYNC_URIS       = "com.soundcloud.android.sync.extra.SYNC_URIS";
    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT     = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_IS_UI_REQUEST   = "com.soundcloud.android.sync.extra.IS_UI_REQUEST";

    public static final int STATUS_SYNC_ERROR      = 0x2;
    public static final int STATUS_SYNC_FINISHED   = 0x3;

    public static final int STATUS_APPEND_ERROR    = 0x4;
    public static final int STATUS_APPEND_FINISHED = 0x5;

    public static final int MAX_TASK_LIMIT = 3;
    private int mActiveTaskCount;
    private Handler mHandler = new Handler();

    /* package */ final List<SyncIntent> mSyncIntents = new ArrayList<SyncIntent>();
    /* package */ final LinkedList<CollectionSyncRequest> mPendingRequests = new LinkedList<CollectionSyncRequest>();
    /* package */ final List<CollectionSyncRequest> mRunningRequests = new ArrayList<CollectionSyncRequest>();


    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "startListening("+intent+")");
        if (intent != null){
            enqueueRequest(new SyncIntent(this, intent));
        }
        flushSyncRequests();
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "onDestroy()");
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.SYNC_STATE,LocalCollection.SyncState.IDLE);
        getContentResolver().update(Content.COLLECTIONS.uri, cv, null, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* package */ void enqueueRequest(SyncIntent syncIntent) {
        mSyncIntents.add(syncIntent);
        for (CollectionSyncRequest request : syncIntent.collectionSyncRequests) {
            if (!mRunningRequests.contains(request)) {
                if (!mPendingRequests.contains(request)) {
                    if (syncIntent.isUIRequest) {
                        mPendingRequests.add(0, request);
                    } else {
                        mPendingRequests.add(request);
                    }
                    request.onQueued();
                } else if (syncIntent.isUIRequest && !mPendingRequests.getFirst().equals(request)) {
                    // move the original object up in the queue, since it has already been initialized with onQueued()
                    final CollectionSyncRequest existing = mPendingRequests.get(mPendingRequests.indexOf(request));
                    mPendingRequests.remove(existing);
                    mPendingRequests.addFirst(existing);
                }
            }
        }
    }

    /* package */ void onUriSyncResult(CollectionSyncRequest syncRequest){
        for (SyncIntent syncIntent : new ArrayList<SyncIntent>(mSyncIntents)) {

            if (syncIntent.onUriResult(syncRequest)){
                mSyncIntents.remove(syncIntent);
            }
        }
        mRunningRequests.remove(syncRequest);
    }

    /* package */ void flushSyncRequests() {
        if (mPendingRequests.isEmpty() && mRunningRequests.isEmpty()) {
            // make sure all sync intents are finished (should have been handled before)
            for (SyncIntent i : mSyncIntents) {
                i.finish();
            }
            stopSelf();
        } else {
            while (mActiveTaskCount < MAX_TASK_LIMIT && !mPendingRequests.isEmpty()) {
                final CollectionSyncRequest syncRequest = mPendingRequests.poll();
                mRunningRequests.add(syncRequest);

                // actual execution of the request
                new ApiTask().executeOnThreadPool(syncRequest);
            }
        }
    }

    private class ApiTask extends ParallelAsyncTask<CollectionSyncRequest, CollectionSyncRequest, Void> {

        public static final int POLL_DELAY = 60 * 1000;
        private Runnable mPoller;
        private int mPollCount;

        @Override
        protected void onPreExecute() {
            mActiveTaskCount++;
        }

        @Override
        protected Void doInBackground(final CollectionSyncRequest... tasks) {
            startPoller(tasks); // must be started from background thread
            for (CollectionSyncRequest task : tasks) {
                publishProgress(task.execute());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(CollectionSyncRequest... progress) {
            for (CollectionSyncRequest request : progress) {
                onUriSyncResult(request);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            mActiveTaskCount--;
            stopPoller();
            flushSyncRequests();
        }

        private void startPoller(CollectionSyncRequest[] tasks) {
            final Thread thread = Thread.currentThread();
            final String uris = Joiner.on(",").join(getTaskUriPaths(tasks));

            mPoller = new Runnable() {
                @Override
                public void run() {
                    mPollCount++;
                    final String msg = "[" + mPollCount + "] " + uris;
                    SoundCloudApplication.handleSilentException(msg, new SyncPollData(msg, thread.getStackTrace()));
                    mHandler.postDelayed(mPoller, POLL_DELAY);
                }
            };
            mHandler.postDelayed(mPoller, POLL_DELAY);
        }

        private void stopPoller() {
            mHandler.removeCallbacks(mPoller);
        }

        private List<String> getTaskUriPaths(CollectionSyncRequest[] tasks) {
            return Lists.transform(Lists.newArrayList(tasks), new Function<CollectionSyncRequest, String>() {
                @Nullable
                @Override
                public String apply(@Nullable CollectionSyncRequest input) {
                    return input.contentUri.getPath();
                }
            });
        }
    }

    public static void appendSyncStats(SyncResult from, SyncResult to) {
        to.stats.numAuthExceptions += from.stats.numAuthExceptions;
        to.stats.numIoExceptions += from.stats.numIoExceptions;
        to.stats.numParseExceptions += from.stats.numParseExceptions;
        // TODO more stats?
    }

    private static class SyncPollData extends Exception {
        private SyncPollData(String message, StackTraceElement[] stackTraceElements) {
            super(message);
            setStackTrace(stackTraceElements);
        }

        @Override
        public String toString() {
            return "SyncPollData :" + getMessage();
        }
    }
}
