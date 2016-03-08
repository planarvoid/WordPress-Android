package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.InvalidTokenException;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.IOException;

@SuppressWarnings({"PMD.AvoidCatchingGenericException"})
@Deprecated // use SyncItem instead
public class LegacySyncJob implements SyncJob {

    public static final String TAG = ApiSyncService.class.getSimpleName();
    private final Uri contentUri;
    private final String action;
    private final boolean isUI;
    private final SyncStateManager syncStateManager;
    private final ApiSyncerFactory apiSyncerFactory;

    private ApiSyncResult result;
    private Exception exception;

    public LegacySyncJob(Uri contentUri, String action, boolean isUI,
                         ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager) {
        this.contentUri = contentUri;
        this.action = action;
        this.isUI = isUI;
        this.syncStateManager = syncStateManager;
        this.apiSyncerFactory = apiSyncerFactory;
        result = new ApiSyncResult(this.contentUri);
    }

    @VisibleForTesting
    public LegacySyncJob(Uri contentUri, String action, boolean isUI) {
        this.contentUri = contentUri;
        this.action = action;
        this.isUI = isUI;
        apiSyncerFactory = null;
        syncStateManager = null;
        result = new ApiSyncResult(this.contentUri);
    }

    @Override
    public void onQueued() {
        if (contentUri != null) {
            fireAndForget(syncStateManager.updateLastSyncAttemptAsync(contentUri));
        } else {
            // Happens with database locking. This should just return with an unsuccessful result below
            Log.e(TAG, "Unable to create collection for null URI");
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
        if (contentUri == null || !syncStateManager.updateLastSyncAttempt(contentUri)) {
            Log.e(TAG, "LocalCollection error :" + contentUri);
            return;
        }

        // make sure all requests going out on this thread have the background parameter set
        PublicApi.setBackgroundMode(!isUI);

        try {
            Log.d(TAG, "syncing " + contentUri);
            result = apiSyncerFactory.forContentUri(contentUri).syncContent(contentUri, action);
            syncStateManager.onSyncComplete(result, contentUri);
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
                throw new IllegalStateException("Unknown error reason : " + exception.reason());
        }
    }

    private void handleSyncException(ApiSyncResult apiSyncResult, Exception exception) {
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

        private final ApiSyncerFactory apiSyncerFactory;
        private final SyncStateManager syncStateManager;

        @Inject
        Factory(ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager) {
            this.apiSyncerFactory = apiSyncerFactory;
            this.syncStateManager = syncStateManager;
        }

        LegacySyncJob create(Uri contentUri, String action, boolean isUI) {
            return new LegacySyncJob(contentUri, action, isUI, apiSyncerFactory, syncStateManager);
        }
    }
}
