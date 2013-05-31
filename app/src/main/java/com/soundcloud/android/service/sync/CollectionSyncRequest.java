package com.soundcloud.android.service.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.service.sync.content.SyncStrategy;
import com.soundcloud.api.CloudAPI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

/**
 * Sync request for one specific collection type. Is queued in the {@link ApiSyncService}, uses {@link ApiSyncer} to do the
 * job, then updates {@link LocalCollection}. The actual execution happens in
 * {@link com.soundcloud.android.service.sync.ApiSyncService#flushSyncRequests()}.
 */
/* package */  class CollectionSyncRequest {

    public static final String TAG = ApiSyncService.class.getSimpleName();

    private final Context context;
    public final Uri contentUri;
    private final String action;
    private final boolean isUI;
    private final SyncStateManager mSyncStateManager;

    private LocalCollection localCollection;
    public ApiSyncResult result;

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.result = new ApiSyncResult(contentUri);
        this.isUI = isUI;
        mSyncStateManager = new SyncStateManager();
    }

    public void onQueued() {
        localCollection = mSyncStateManager.fromContent(contentUri);
        if (localCollection != null) {
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.PENDING);
        } else {
            // Happens with database locking. This should just return with an unsuccessful result below
            Log.e(TAG, "Unable to create collection for uri " + contentUri);
        }
    }

    /**
     * Execute the sync request. This should happen on a separate worker thread.
     */
    public CollectionSyncRequest execute() {
        if (localCollection == null) {
            Log.e(TAG, "No local collection available :" + contentUri);
            return this;
        }

        // make sure all requests going out on this thread have the background parameter set
        Wrapper.setBackgroundMode(!isUI);

        SyncStrategy syncer = new ApiSyncerFactory().forContentUri(context, contentUri);

        if (!mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.SYNCING)) {
            return this;
        }

        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "syncing " + contentUri);
            result = syncer.syncContent(contentUri, action);
            mSyncStateManager.onSyncComplete(result, localCollection);

        } catch (CloudAPI.InvalidTokenException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromAuthException(contentUri);
            context.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
        } catch (IOException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            result = ApiSyncResult.fromIOException(contentUri);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            Wrapper.setBackgroundMode(false);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "executed sync on " + this);
        }
        return this;
    }

    @Override @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionSyncRequest that = (CollectionSyncRequest) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (contentUri != null ? !contentUri.equals(that.contentUri) : that.contentUri != null) return false;
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
}
