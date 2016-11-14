package com.soundcloud.android.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.Log;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ApiSyncService extends Service {
    public static final String LOG_TAG = ApiSyncService.class.getSimpleName();

    public static final String EXTRA_SYNCABLE = "com.soundcloud.android.sync.extra.SYNCABLE";
    public static final String EXTRA_SYNCABLES = "com.soundcloud.android.sync.extra.SYNCABLES";
    public static final String EXTRA_SYNCABLE_ENTITIES = "com.soundcloud.android.sync.extra.SYNCABLE_ENTITIES";

    public static final String ACTION_APPEND = "com.soundcloud.android.sync.action.APPEND";
    public static final String ACTION_HARD_REFRESH = "com.soundcloud.android.sync.action.HARD_REFRESH";

    public static final String EXTRA_SYNC_URIS = "com.soundcloud.android.sync.extra.SYNC_URIS";
    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_IS_UI_REQUEST = "com.soundcloud.android.sync.extra.IS_UI_REQUEST";

    public static final int STATUS_SYNC_ERROR = 0x2;
    public static final int STATUS_SYNC_FINISHED = 0x3;

    public static final int STATUS_APPEND_ERROR = 0x4;
    public static final int STATUS_APPEND_FINISHED = 0x5;

    private static final int MAX_TASK_LIMIT = 3;

    @Inject SyncRequestFactory syncIntentSyncRequestFactory;
    @Inject SyncStateStorage syncStateStorage;
    @Inject UnauthorisedRequestRegistry unauthorisedRequestRegistry;

    private int activeTaskCount;

    @SuppressWarnings({"PMD.LooseCoupling"})
    private final LinkedList<SyncJob> pendingJobs = new LinkedList<>();
    private final List<SyncRequest> syncRequests = new ArrayList<>();
    private final List<SyncJob> runningJobs = new ArrayList<>();

    public ApiSyncService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ApiSyncService(SyncRequestFactory syncRequestFactory,
                   SyncStateStorage syncStateStorage,
                   UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        this.syncIntentSyncRequestFactory = syncRequestFactory;
        this.syncStateStorage = syncStateStorage;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "startListening(" + intent + ")");
        if (intent != null) {
            SyncRequest syncRequest = syncIntentSyncRequestFactory.create(intent);
            syncRequests.add(syncRequest);

            if (shouldEnqueueJobs(intent)) {
                enqueueRequest(syncRequest);
            }
        }
        flushSyncRequests();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @VisibleForTesting
    void onSyncJobCompleted(SyncJob syncJob) {

        final Optional<Syncable> syncable = syncJob.getSyncable();
        if (syncable.isPresent() && syncJob.wasSuccess()) {
            syncStateStorage.synced(syncable.get());
        }

        for (SyncRequest syncRequest : new ArrayList<>(syncRequests)) {

            if (syncRequest.isWaitingForJob(syncJob)) {
                syncRequest.processJobResult(syncJob);

                if (syncRequest.isSatisfied()) {
                    syncRequest.finish();
                    syncRequests.remove(syncRequest);
                }
            }
        }
        runningJobs.remove(syncJob);
    }

    @VisibleForTesting
    boolean shouldEnqueueJobs(Intent intent) {
        return !isUiRequestExtraDefined(intent) ||
                isUiRequest(intent) ||
                isAuthTokenValid();
    }

    // Adding additional check to be SURE this is a background sync request.
    // Otherwise, foreground request might not end up getting queued and
    // thus TokenExpiredDialogFragment won't be presented when it should.
    private boolean isUiRequestExtraDefined(Intent intent) {
        Bundle extras = intent.getExtras();
        return extras != null && extras.containsKey(ApiSyncService.EXTRA_IS_UI_REQUEST);
    }

    private boolean isUiRequest(Intent intent) {
        return intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false);
    }

    private boolean isAuthTokenValid() {
        return !unauthorisedRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit();
    }

    private void enqueueRequest(SyncRequest syncRequest) {
        for (SyncJob syncJob : syncRequest.getPendingJobs()) {
            if (!runningJobs.contains(syncJob)) {
                if (!pendingJobs.contains(syncJob)) {
                    addItemToPendingRequests(syncRequest, syncJob);
                    syncJob.onQueued();
                } else if (syncRequest.isHighPriority()) {
                    moveRequestToTop(syncJob);
                }
            } else {
                Log.d(LOG_TAG, "Job already running for : " + syncJob);
            }
        }
    }

    private void addItemToPendingRequests(SyncRequest syncRequest, SyncJob syncJob) {
        Log.d(LOG_TAG, "Adding sync job to queue : " + syncJob);
        if (syncRequest.isHighPriority()) {
            pendingJobs.add(0, syncJob);
        } else {
            pendingJobs.add(syncJob);
        }
    }

    private void moveRequestToTop(SyncJob syncJob) {
        Log.d(LOG_TAG, "Moving sync job to front of queue : " + syncJob);

        final SyncJob existing = pendingJobs.get(pendingJobs.indexOf(syncJob));
        pendingJobs.remove(existing);
        pendingJobs.addFirst(existing);
    }

    private void flushSyncRequests() {
        if (pendingJobs.isEmpty() && runningJobs.isEmpty()) {
            finishAllRequests();
            stopSelf();
        } else {
            while (activeTaskCount < MAX_TASK_LIMIT && !pendingJobs.isEmpty()) {
                final SyncJob syncJob = pendingJobs.poll();
                runningJobs.add(syncJob);

                // actual execution of the request
                new ApiTask().executeOnThreadPool(syncJob);
            }
        }
    }

    private void finishAllRequests() {
        // make sure all sync intents are finished (should have been handled before)
        for (SyncRequest syncRequest : syncRequests) {
            syncRequest.finish();
        }
    }

    private class ApiTask extends ParallelAsyncTask<SyncJob, SyncJob, Void> {

        @Override
        protected void onPreExecute() {
            activeTaskCount++;
        }

        @Override
        protected Void doInBackground(final SyncJob... syncJobs) {
            for (SyncJob syncJob : syncJobs) {
                syncJob.run();
                publishProgress(syncJob);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(SyncJob... progress) {
            for (SyncJob syncJob : progress) {
                onSyncJobCompleted(syncJob);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            activeTaskCount--;
            flushSyncRequests();
        }
    }
}
