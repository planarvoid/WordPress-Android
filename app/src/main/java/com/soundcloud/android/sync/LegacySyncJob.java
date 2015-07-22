package com.soundcloud.android.sync;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.InvalidTokenException;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

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
@Deprecated // use SyncItem instead
public class LegacySyncJob implements SyncJob {

    public static final String TAG = ApiSyncService.class.getSimpleName();
    private final Context context;
    private final Uri contentUri;
    private final String action;
    private final boolean isUI;
    private final SyncStateManager syncStateManager;
    private final ApiSyncerFactory apiSyncerFactory;

    private LocalCollection localCollection;
    private ApiSyncResult result;
    private Exception exception;

    public LegacySyncJob(Context context, Uri contentUri, String action, boolean isUI,
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
    public LegacySyncJob(Context context, Uri contentUri, String action, boolean isUI) {
        this.context = context;
        this.contentUri = contentUri;
        this.action = action;
        this.isUI = isUI;
        apiSyncerFactory = null;
        syncStateManager = null;
        result = new ApiSyncResult(this.contentUri);
    }

    @Override
    public void onQueued() {
        localCollection = syncStateManager.fromContent(contentUri);
        if (localCollection != null) {
            syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.PENDING);
        } else {
            // Happens with database locking. This should just return with an unsuccessful result below
            Log.e(TAG, "Unable to create collection for uri " + contentUri);
        }
    }

    @Override
    public boolean resultedInAChange() {
        return result.change == ApiSyncResult.CHANGED;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    /**
     * Execute the sync request. This should happen on a separate worker thread.
     */
    @Override
    public void run() {
        if (localCollection == null || !syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.SYNCING)) {
            Log.e(TAG, "LocalCollection error :" + contentUri);
            return;
        }

        // make sure all requests going out on this thread have the background parameter set
        PublicApi.setBackgroundMode(!isUI);

        try {
            Log.d(TAG, "syncing " + contentUri);
            result = apiSyncerFactory.forContentUri(context, contentUri).syncContent(contentUri, action);
            syncStateManager.onSyncComplete(result, localCollection);
        } catch (InvalidTokenException e) {
            handleSyncException(ApiSyncResult.fromAuthException(contentUri), e);
        } catch (UnexpectedResponseException e) {
            handleSyncException(ApiSyncResult.fromUnexpectedResponse(contentUri, e.getStatusCode()), e);
        } catch (IOException e) {
            handleSyncException(ApiSyncResult.fromIOException(contentUri), e);
        } catch (ApiRequestException exception) {
            handleApiRequestException(exception);
        } catch (Exception e) {
            ErrorUtils.handleSilentException(e);
            handleSyncException(ApiSyncResult.fromGeneralFailure(contentUri), e);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            PublicApi.setBackgroundMode(false);
        }

        Log.d(TAG, "executed sync on " + this);
    }

    private void handleApiRequestException(ApiRequestException exception) {
        switch (exception.reason()) {
            case AUTH_ERROR:
            case NOT_ALLOWED:
                handleSyncException(ApiSyncResult.fromAuthException(contentUri), exception);
                break;
            case NETWORK_ERROR:
                handleSyncException(ApiSyncResult.fromIOException(contentUri), exception);
                break;
            case SERVER_ERROR:
                handleSyncException(ApiSyncResult.fromServerError(contentUri), exception);
                break;
            case UNEXPECTED_RESPONSE:
            case BAD_REQUEST:
            case MALFORMED_INPUT:
                ErrorUtils.handleSilentException(exception);
                // do not break
            case NOT_FOUND:
            case RATE_LIMITED:
                handleSyncException(ApiSyncResult.fromClientError(contentUri), exception);
                break;
            default:
                throw new IllegalStateException("Umknown error reason : " + exception.reason());
        }
    }

    private void handleSyncException(ApiSyncResult apiSyncResult, Exception exception) {
        syncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
        result = apiSyncResult;
        this.exception = exception;
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

        LegacySyncJob that = (LegacySyncJob) o;

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

        LegacySyncJob create(Uri contentUri, String action, boolean isUI) {
            return new LegacySyncJob(context, contentUri, action, isUI, apiSyncerFactory, syncStateManager);
        }
    }
}
