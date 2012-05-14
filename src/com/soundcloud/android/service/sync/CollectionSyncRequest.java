package com.soundcloud.android.service.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.api.CloudAPI;

import java.io.IOException;

/**
 * Sync request for one specific collection type. Is queued in the {@link ApiSyncService}, uses {@link ApiSyncer} to do the
 * job, then updates {@link LocalCollection}.
 */
/* package */  class CollectionSyncRequest {

    public static final String TAG = ApiSyncService.class.getSimpleName();

    private final Context context;
    public final Uri contentUri;
    private final String action;
    private LocalCollection localCollection;

    public ApiSyncer.Result result;
    public boolean hasBeenQueued;

    public CollectionSyncRequest(Context context, Uri contentUri, String action) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.result = new ApiSyncer.Result(contentUri);
    }

    public void onQueued() {
        localCollection = LocalCollection.fromContentUri(contentUri, context.getContentResolver(), true);
        localCollection.updateSyncState(LocalCollection.SyncState.PENDING, context.getContentResolver());
        hasBeenQueued = true;
    }


    public CollectionSyncRequest execute() {
        if (localCollection == null) throw new IllegalStateException("request has not been queued");

        ApiSyncer syncer = new ApiSyncer(context);
        localCollection.updateSyncState(LocalCollection.SyncState.SYNCING, context.getContentResolver());
        try {
            result = syncer.syncContent(contentUri, action);
            localCollection.onSyncComplete(result, context.getContentResolver());
        } catch (CloudAPI.InvalidTokenException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, context.getContentResolver());
            result = ApiSyncer.Result.fromAuthException(contentUri);
        } catch (IOException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, context.getContentResolver());
            result = ApiSyncer.Result.fromIOException(contentUri);
        }

        if (Log.isLoggable(TAG,Log.DEBUG)){
            Log.d(TAG,"Executed sync on " + toString());
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
