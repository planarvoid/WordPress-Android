package com.soundcloud.android.service.sync;

import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import com.soundcloud.android.SoundCloudApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A collection of one or more content uris to be synced. Used by the {@link ApiSyncService} to coordinate intent fulfillment.
 * In the common case only one uri is requested, unless the sync is initiated by the {@link SyncAdapterService}.
 *
 * The sync result is optionally communicated back to the caller via a {@link ResultReceiver}.
 *
 * The action of the passed intent is either {@link Intent.ACTION_SYNC} or {@link ApiSyncService.ACTION_APPEND}
 * (for Activities).
 */
/* package */  class ApiSyncServiceRequest {
    private final String action;
    public final List<CollectionSyncRequest> collectionSyncRequests = new ArrayList<CollectionSyncRequest>();
    private final Set<CollectionSyncRequest> requestsRemaining;

    public final boolean isUIRequest; // used for queueing priorities

    // results
    private final ResultReceiver resultReceiver;
    private final Bundle resultData = new Bundle();
    private final SyncResult syncAdapterResult = new SyncResult();

    public ApiSyncServiceRequest(SoundCloudApplication application, Intent intent) {
        resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        isUIRequest = intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false);
        action = intent.getAction();

        ArrayList<Uri> syncUris = intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        if (syncUris == null) syncUris = new ArrayList<Uri>();

        if (intent.getData() != null) {
            syncUris.add(intent.getData());
        }
        for (Uri uri : syncUris) {
            collectionSyncRequests.add(new CollectionSyncRequest(application, uri, action));
        }
        requestsRemaining = new HashSet<CollectionSyncRequest>(collectionSyncRequests);
    }


    /**
     * @return true if all requests have been processed, otherwise false.
     */
    public boolean onUriResult(CollectionSyncRequest request) {
        if (request.result == null) return false;

        if (requestsRemaining.contains(request)) {
            requestsRemaining.remove(request);
            resultData.putBoolean(request.contentUri.toString(), isUIRequest ?
                    request.result.change != ApiSyncer.Result.UNCHANGED : request.result.change == ApiSyncer.Result.CHANGED);

            if (!request.result.success) ApiSyncService.appendSyncStats(request.result.syncResult, this.syncAdapterResult);
        }

        if (requestsRemaining.isEmpty()) {
            if (resultReceiver != null) {
                if (request.result.success) {
                    resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_FINISHED : ApiSyncService.STATUS_SYNC_FINISHED, resultData);
                } else {
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncAdapterResult);
                    resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_ERROR : ApiSyncService.STATUS_SYNC_ERROR, bundle);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
