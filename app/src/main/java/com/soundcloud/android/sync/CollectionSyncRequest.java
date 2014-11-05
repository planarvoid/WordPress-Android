package com.soundcloud.android.sync;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.CloudAPI;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Sync request for one specific collection type. Is queued in the {@link ApiSyncService}, uses {@link ApiSyncer} to do the
 * job, then updates {@link LocalCollection}. The actual execution happens in
 * {@link com.soundcloud.android.sync.ApiSyncService#flushSyncRequests()}.
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException"})
/* package */  class CollectionSyncRequest {

    public static final String TAG = ApiSyncService.class.getSimpleName();
    private final Context context;
    private final Uri contentUri;
    private final String action;
    private final boolean isUI;
    private final SyncStateManager syncStateManager;
    private final ApiSyncerFactory apiSyncerFactory;

    private LocalCollection localCollection;
    private ApiSyncResult result;

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI,
                                 ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.isUI = isUI;
        this.syncStateManager = syncStateManager;
        this.apiSyncerFactory = apiSyncerFactory;
        result = new ApiSyncResult(this.contentUri);
    }

    @VisibleForTesting
    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.isUI = isUI;
        apiSyncerFactory = null;
        syncStateManager = null;
        result = new ApiSyncResult(this.contentUri);
    }

    public void onQueued() {
        localCollection = syncStateManager.fromContent(contentUri);
        if (localCollection != null) {
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.PENDING);
        } else {
            // Happens with database locking. This should just return with an unsuccessful result below
            Log.e(TAG, "Unable to create collection for uri " + contentUri);
        }
    }

    /**
     * Execute the sync request. This should happen on a separate worker thread.
     */
    public CollectionSyncRequest execute() {
        if (localCollection == null || !syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.SYNCING)) {
            Log.e(TAG, "LocalCollection error :" + contentUri);
            return this;
        }

        // make sure all requests going out on this thread have the background parameter set
        PublicApiWrapper.setBackgroundMode(!isUI);

        try {
            Log.d(TAG, "syncing " + contentUri);
            result = apiSyncerFactory.forContentUri(context, contentUri).syncContent(contentUri, action);
            syncStateManager.onSyncComplete(result, localCollection);
        } catch (CloudAPI.InvalidTokenException e) {
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromAuthException(contentUri);
        } catch (PublicCloudAPI.UnexpectedResponseException e) {
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromUnexpectedResponseException(contentUri);
        } catch (IOException e) {
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromIOException(contentUri);
        } catch (RuntimeException ex) {
            ErrorUtils.handleSilentException(ex);
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromUnexpectedResponseException(contentUri);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            PublicApiWrapper.setBackgroundMode(false);
        }

        Log.d(TAG, "executed sync on " + this);
        return this;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public ApiSyncResult getResult() {
        return result;
    }

    public void setResult(ApiSyncResult result) {
        this.result = result;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CollectionSyncRequest that = (CollectionSyncRequest) o;

        if (action != null ? !action.equals(that.action) : that.action != null) {
            return false;
        }
        if (contentUri != null ? !contentUri.equals(that.contentUri) : that.contentUri != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = contentUri != null ? contentUri.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CollectionSyncRequest{" +
                "contentUri=" + contentUri +
                ", action='" + action + '\'' +
                ", result=" + result +
                '}';
    }


    static class Factory {

        private final Context context;
        private final ApiSyncerFactory apiSyncerFactory;
        private final SyncStateManager syncStateManager;

        @Inject
        Factory(Context context, ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager) {
            this.context = context;
            this.apiSyncerFactory = apiSyncerFactory;
            this.syncStateManager = syncStateManager;
        }

        CollectionSyncRequest create(Uri contentUri, String action, boolean isUI) {
            return new CollectionSyncRequest(context, contentUri, action, isUI, apiSyncerFactory, syncStateManager);
        }
    }

}
