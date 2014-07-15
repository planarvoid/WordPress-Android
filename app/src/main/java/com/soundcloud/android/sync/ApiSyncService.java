package com.soundcloud.android.sync;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.Log;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncResult;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ApiSyncService extends Service {
    public static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    public static final String ACTION_APPEND        = "com.soundcloud.android.sync.action.APPEND";
    public static final String ACTION_PUSH          = "com.soundcloud.android.sync.action.PUSH";
    public static final String ACTION_HARD_REFRESH  = "com.soundcloud.android.sync.action.HARD_REFRESH";

    public static final String EXTRA_SYNC_URIS       = "com.soundcloud.android.sync.extra.SYNC_URIS";
    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT     = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_IS_UI_REQUEST   = "com.soundcloud.android.sync.extra.IS_UI_REQUEST";

    public static final int STATUS_SYNC_ERROR      = 0x2;
    public static final int STATUS_SYNC_FINISHED   = 0x3;

    public static final int STATUS_APPEND_ERROR    = 0x4;
    public static final int STATUS_APPEND_FINISHED = 0x5;

    public static final int MAX_TASK_LIMIT = 3;

    private int activeTaskCount;

    /* package */ final List<SyncIntent> syncIntents = new ArrayList<SyncIntent>();
    /* package */ final LinkedList<CollectionSyncRequest> pendingRequests = new LinkedList<CollectionSyncRequest>();
    /* package */ final List<CollectionSyncRequest> runningRequests = new ArrayList<CollectionSyncRequest>();

    @Override
    public void onCreate() {
        super.onCreate();
        // We have to make sure the follow cache is instantiated on the UI thread, or the syncer could cause a crash
        // TODO, remove this once we get rid of FollowStatus
        FollowingOperations.init();
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(LOG_TAG, "startListening("+intent+")");
        if (intent != null){
            enqueueRequest(new SyncIntent(this, intent));
        }
        flushSyncRequests();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.SYNC_STATE,LocalCollection.SyncState.IDLE);
        getContentResolver().update(Content.COLLECTIONS.uri, cv, null, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* package */ void enqueueRequest(SyncIntent syncIntent) {
        syncIntents.add(syncIntent);
        for (CollectionSyncRequest request : syncIntent.collectionSyncRequests) {
            if (!runningRequests.contains(request)) {
                if (!pendingRequests.contains(request)) {
                    if (syncIntent.isUIRequest) {
                        pendingRequests.add(0, request);
                    } else {
                        pendingRequests.add(request);
                    }
                    request.onQueued();
                } else if (syncIntent.isUIRequest && !pendingRequests.getFirst().equals(request)) {
                    // move the original object up in the queue, since it has already been initialized with onQueued()
                    final CollectionSyncRequest existing = pendingRequests.get(pendingRequests.indexOf(request));
                    pendingRequests.remove(existing);
                    pendingRequests.addFirst(existing);
                }
            }
        }
    }

    /* package */ void onUriSyncResult(CollectionSyncRequest syncRequest){
        for (SyncIntent syncIntent : new ArrayList<SyncIntent>(syncIntents)) {

            if (syncIntent.onUriResult(syncRequest)){
                syncIntents.remove(syncIntent);
            }
        }
        runningRequests.remove(syncRequest);
    }

    /* package */ void flushSyncRequests() {
        if (pendingRequests.isEmpty() && runningRequests.isEmpty()) {
            // make sure all sync intents are finished (should have been handled before)
            for (SyncIntent i : syncIntents) {
                i.finish();
            }
            stopSelf();
        } else {
            while (activeTaskCount < MAX_TASK_LIMIT && !pendingRequests.isEmpty()) {
                final CollectionSyncRequest syncRequest = pendingRequests.poll();
                runningRequests.add(syncRequest);

                // actual execution of the request
                new ApiTask().executeOnThreadPool(syncRequest);
            }
        }
    }

    private class ApiTask extends ParallelAsyncTask<CollectionSyncRequest, CollectionSyncRequest, Void> {

        @Override
        protected void onPreExecute() {
            activeTaskCount++;
        }

        @Override
        protected Void doInBackground(final CollectionSyncRequest... tasks) {
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
            activeTaskCount--;
            flushSyncRequests();
        }
    }

    public static void appendSyncStats(SyncResult from, SyncResult to) {
        to.stats.numAuthExceptions += from.stats.numAuthExceptions;
        to.stats.numIoExceptions += from.stats.numIoExceptions;
        to.stats.numParseExceptions += from.stats.numParseExceptions;
        // TODO more stats?
    }
}
