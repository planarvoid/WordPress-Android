package com.soundcloud.android.sync;

import static com.soundcloud.android.sync.ApiSyncer.TAG;

import com.soundcloud.android.utils.Log;

import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A collection of one or more content uris to be synced. Used by the {@link ApiSyncService} to coordinate intent fulfillment.
 * In the common case only one uri is requested, unless the sync is initiated by the {@link SyncAdapterService}.
 * <p/>
 * The sync result is optionally communicated back to the caller via a {@link ResultReceiver}.
 * <p/>
 * The action of the passed intent is either {@link Intent#ACTION_SYNC} or {@link ApiSyncService#ACTION_APPEND}
 * (for Activities).
 */
@Deprecated
/* package */  class LegacySyncRequest implements SyncRequest<LegacySyncJob> {
    private final String action;
    private final List<LegacySyncJob> legacySyncItems = new ArrayList<LegacySyncJob>();
    private final Set<LegacySyncJob> requestsRemaining;

    private final boolean isUIRequest; // used for queueing priorities

    // results
    private final ResultReceiver resultReceiver;
    private final Bundle resultData = new Bundle();
    private final SyncResult syncAdapterResult = new SyncResult();

    LegacySyncRequest(Intent intent, LegacySyncJob.Factory collectionSyncRequestFactory) {
        resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        isUIRequest = intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false);
        action = intent.getAction();

        ArrayList<Uri> syncUris = intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        if (syncUris == null) {
            syncUris = new ArrayList<Uri>();
        }

        if (intent.getData() != null) {
            syncUris.add(intent.getData());
        }
        for (Uri uri : syncUris) {
            legacySyncItems.add(collectionSyncRequestFactory.create(uri, action, isUIRequest));
        }
        requestsRemaining = new HashSet<LegacySyncJob>(legacySyncItems);
    }


    public void finish() {
        if (resultReceiver != null) {
            if (isSuccess()) {
                resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_FINISHED : ApiSyncService.STATUS_SYNC_FINISHED, resultData);
            } else {
                final Bundle bundle = new Bundle();
                bundle.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncAdapterResult);
                resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_ERROR : ApiSyncService.STATUS_SYNC_ERROR, bundle);
            }
        }
    }

    private boolean isSuccess() {
        for (LegacySyncJob r : legacySyncItems) {
            if (!r.getResult().success) {
                Log.w(TAG, "collection sync request " + r + " not successful");
                return false;
            }
        }
        return true;
    }

    static class Factory {
        private final LegacySyncJob.Factory collectionSyncRequestFactory;

        @Inject
        Factory(LegacySyncJob.Factory collectionSyncRequestFactory) {
            this.collectionSyncRequestFactory = collectionSyncRequestFactory;
        }

        LegacySyncRequest create(Intent intent){
            return new LegacySyncRequest(intent, collectionSyncRequestFactory);
        }
    }

    @Override
    public boolean isHighPriority() {
        return isUIRequest;
    }

    @Override
    public Collection<LegacySyncJob> getPendingJobs() {
        return legacySyncItems;
    }

    @Override
    public boolean isWaitingForJob(LegacySyncJob syncJob) {
        return requestsRemaining.contains(syncJob);
    }

    @Override
    public void processJobResult(LegacySyncJob syncJob) {
        // if this is a different instance of the same sync request, share the result
        for (LegacySyncJob instance : requestsRemaining) {
            if (instance.equals(syncJob) && instance != syncJob) {
                instance.setResult(syncJob.getResult());
            }
        }
        requestsRemaining.remove(syncJob);

        resultData.putBoolean(syncJob.getContentUri().toString(), isUIRequest ?
                syncJob.getResult().change != ApiSyncResult.UNCHANGED : syncJob.getResult().change == ApiSyncResult.CHANGED);

        if (!syncJob.getResult().success) {
            syncAdapterResult.stats.numAuthExceptions += syncJob.getResult().syncResult.stats.numAuthExceptions;
            syncAdapterResult.stats.numIoExceptions += syncJob.getResult().syncResult.stats.numIoExceptions;
            syncAdapterResult.stats.numParseExceptions += syncJob.getResult().syncResult.stats.numParseExceptions;
            // TODO more stats?
        }
    }

    @Override
    public boolean isSatisfied() {
        return requestsRemaining.isEmpty();
    }
}
