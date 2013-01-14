package com.soundcloud.android.service.sync;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.LocalCollection;
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

    private LocalCollection localCollection;
    public ApiSyncer.Result result;

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.result = new ApiSyncer.Result(contentUri);
        this.isUI = isUI;
    }

    public void onQueued() {
        localCollection = LocalCollection.fromContentUri(contentUri, context.getContentResolver(), true);
        localCollection.updateSyncState(LocalCollection.SyncState.PENDING, context.getContentResolver());
    }

    /**
     * Execute the sync request. This should happen on a separate worker thread.
     * @return
     */
    public CollectionSyncRequest execute() {
        if (localCollection == null) throw new IllegalStateException("request has not been queued");

        // make sure all requests going out on this thread have the background parameter set
        AndroidCloudAPI.Wrapper.setBackgroundMode(!isUI);

        ApiSyncer syncer = new ApiSyncer(context);

        if (!localCollection.updateSyncState(LocalCollection.SyncState.SYNCING, context.getContentResolver())){
            return this;
        }

        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "syncing " + contentUri);

            result = syncer.syncContent(contentUri, action);
            localCollection.onSyncComplete(result, context.getContentResolver());
        } catch (CloudAPI.InvalidTokenException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, context.getContentResolver());
            result = ApiSyncer.Result.fromAuthException(contentUri);
            context.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
        } catch (IOException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, context.getContentResolver());
            result = ApiSyncer.Result.fromIOException(contentUri);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            AndroidCloudAPI.Wrapper.setBackgroundMode(false);
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
