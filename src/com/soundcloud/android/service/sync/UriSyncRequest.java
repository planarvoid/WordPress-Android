package com.soundcloud.android.service.sync;

import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.api.CloudAPI;

import java.io.IOException;

/* package */  class UriSyncRequest {
    private final SoundCloudApplication app;
    final Uri uri;
    private final String action;
    private LocalCollection localCollection;

    public ApiSyncer.Result result;
    // results

    public UriSyncRequest(SoundCloudApplication application, Uri uri, String action){
        this.app = application;
        this.uri = uri;
        this.action = action;
    }

    public void init(boolean setIsPending){
        localCollection = LocalCollection.fromContentUri(uri,app.getContentResolver(),true);
        if (setIsPending){
            localCollection.updateSyncState(LocalCollection.SyncState.PENDING, app.getContentResolver());
        }
    }

    public UriSyncRequest execute() {
        ApiSyncer syncer = new ApiSyncer(app);
        localCollection.updateSyncState(LocalCollection.SyncState.SYNCING, app.getContentResolver());
        try {
            result = syncer.syncContent(uri, action);
            localCollection.onSyncComplete(result, app.getContentResolver());
            result.success = true;
        } catch (CloudAPI.InvalidTokenException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
            result = ApiSyncer.Result.fromAuthException(uri);
        } catch (IOException e) {
            Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
            localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
            result = ApiSyncer.Result.fromIOException(uri);
        }
        return this;
    }

    @Override @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UriSyncRequest that = (UriSyncRequest) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    public Uri getUri(){
        return uri;
    }

    @Override
    public String toString() {
        return "UriRequest{" +
                "uri=" + uri +
                ", action='" + action + '\'' +
                '}';
    }
}
