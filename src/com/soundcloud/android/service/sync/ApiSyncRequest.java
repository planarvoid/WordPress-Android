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

/* package */  class ApiSyncRequest {
    private final ResultReceiver resultReceiver;
    private final String action;
    private final Set<UriSyncRequest> requestsRemaining;

    public final List<UriSyncRequest> uriRequests;
    public boolean isUIResponse;

    // results
    final Bundle resultData = new Bundle();
    private final SyncResult requestResult = new SyncResult();

    public ApiSyncRequest(SoundCloudApplication application, Intent intent) {
        resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        isUIResponse = intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_RESPONSE, false);
        action = intent.getAction();

        final List<Uri> urisToSync = ApiSyncService.getUrisToSync(intent);
        uriRequests = new ArrayList<UriSyncRequest>();
        for (Uri uri : urisToSync){
            uriRequests.add(new UriSyncRequest(application,uri,action));
        }
        requestsRemaining = new HashSet<UriSyncRequest>(uriRequests);
    }

    public boolean onUriResult(UriSyncRequest uriRequest) {
        if (requestsRemaining.contains(uriRequest)) {
            requestsRemaining.remove(uriRequest);
            resultData.putBoolean(uriRequest.uri.toString(), isUIResponse ? uriRequest.result.requiresUiRefresh : uriRequest.result.wasChanged);
            if (!uriRequest.result.success) ApiSyncService.appendSyncStats(uriRequest.result.syncResult, this.requestResult);
        }

        if (requestsRemaining.isEmpty()) {
            if (resultReceiver != null) {
                if (uriRequest.result.success) {
                    resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_FINISHED : ApiSyncService.STATUS_SYNC_FINISHED, resultData);
                } else {
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, requestResult);
                    resultReceiver.send(ApiSyncService.ACTION_APPEND.equals(action) ? ApiSyncService.STATUS_APPEND_ERROR : ApiSyncService.STATUS_SYNC_ERROR, bundle);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
