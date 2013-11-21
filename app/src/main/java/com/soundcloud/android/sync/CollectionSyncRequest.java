package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.CloudAPI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;

/**
 * Sync request for one specific collection type. Is queued in the {@link ApiSyncService}, uses {@link ApiSyncer} to do the
 * job, then updates {@link LocalCollection}. The actual execution happens in
 * {@link com.soundcloud.android.sync.ApiSyncService#flushSyncRequests()}.
 */
/* package */  class CollectionSyncRequest {

    public static final String TAG = ApiSyncService.class.getSimpleName();
    private final Context mContext;
    private final Uri mContentUri;
    private final String mAction;
    private final boolean mIsUi;
    private final SyncStateManager mSyncStateManager;
    private ApiSyncerFactory mApiSyncerFactory;

    private LocalCollection localCollection;
    private ApiSyncResult mResult;

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI){
        this(context, contentUri, action, isUI, new ApiSyncerFactory(), new SyncStateManager(context));
    }

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI,
                                 ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager) {
        mContext = context;
        mContentUri = contentUri;
        mAction = action;
        mResult = new ApiSyncResult(mContentUri);
        mIsUi = isUI;
        mSyncStateManager = syncStateManager;
        mApiSyncerFactory = apiSyncerFactory;
    }

    public void onQueued() {
        localCollection = mSyncStateManager.fromContent(mContentUri);
        if (localCollection != null) {
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.PENDING);
        } else {
            // Happens with database locking. This should just return with an unsuccessful result below
            Log.e(TAG, "Unable to create collection for uri " + mContentUri);
        }
    }

    /**
     * Execute the sync request. This should happen on a separate worker thread.
     */
    public CollectionSyncRequest execute() {
        if (localCollection == null || !mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.SYNCING)) {
            Log.e(TAG, "LocalCollection error :" + mContentUri);
            return this;
        }

        // make sure all requests going out on this thread have the background parameter set
        PublicApiWrapper.setBackgroundMode(!mIsUi);

        try {
            Log.d(TAG, "syncing " + mContentUri);
            mResult = mApiSyncerFactory.forContentUri(mContext, mContentUri).syncContent(mContentUri, mAction);
            mSyncStateManager.onSyncComplete(mResult, localCollection);
        } catch (CloudAPI.InvalidTokenException e) {
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            mResult = ApiSyncResult.fromAuthException(mContentUri);
            mContext.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));

        } catch (PublicCloudAPI.UnexpectedResponseException e) {
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            mResult = ApiSyncResult.fromUnexpectedResponseException(mContentUri);

        } catch (IOException e) {
            mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
            mResult = ApiSyncResult.fromIOException(mContentUri);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            PublicApiWrapper.setBackgroundMode(false);
        }

        Log.d(TAG, "executed sync on " + this);
        return this;
    }

    public Uri getContentUri() {
        return mContentUri;
    }

    public ApiSyncResult getResult() {
        return mResult;
    }

    public void setmResult(ApiSyncResult mResult) {
        this.mResult = mResult;
    }

    @Override @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionSyncRequest that = (CollectionSyncRequest) o;

        if (mAction != null ? !mAction.equals(that.mAction) : that.mAction != null) return false;
        if (mContentUri != null ? !mContentUri.equals(that.mContentUri) : that.mContentUri != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = mContentUri != null ? mContentUri.hashCode() : 0;
        result = 31 * result + (mAction != null ? mAction.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CollectionSyncRequest{" +
                "contentUri=" + mContentUri +
                ", action='" + mAction + '\'' +
                ", result=" + mResult +
                '}';
    }


}
