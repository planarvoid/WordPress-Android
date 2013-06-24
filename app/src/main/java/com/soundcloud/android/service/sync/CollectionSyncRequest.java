package com.soundcloud.android.service.sync;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.api.CloudAPI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * Sync request for one specific collection type. Is queued in the {@link ApiSyncService}, uses {@link ApiSyncer} to do the
 * job, then updates {@link LocalCollection}. The actual execution happens in
 * {@link com.soundcloud.android.service.sync.ApiSyncService#flushSyncRequests()}.
 */
/* package */  class CollectionSyncRequest {

    public static final String TAG = ApiSyncService.class.getSimpleName();
    public static final int SYNC_REPEAT_TOLERANCE = 10 * 60 * 1000;
    public static final String PREFIX_LAST_SYNC_RESULT = "last_sync_result_";

    public static final String PREF_VAL_SUCCESS = "success";
    public static final String PREF_VAL_FAILED = "failed";
    public static final String PREF_VAL_NULL = "[null]";

    private final Context mContext;
    private final Uri mContentUri;
    private final String mAction, mResultKey;
    private final boolean mIsUi;
    private final SyncStateManager mSyncStateManager;
    private ApiSyncerFactory mApiSyncerFactory;
    private SharedPreferences mSharedPreferences;

    private LocalCollection localCollection;
    private ApiSyncResult mResult;

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI){
        this(context, contentUri, action, isUI, new ApiSyncerFactory(), new SyncStateManager(), PreferenceManager.getDefaultSharedPreferences(context));
    }

    public CollectionSyncRequest(Context context, Uri contentUri, String action, boolean isUI,
                                 ApiSyncerFactory apiSyncerFactory, SyncStateManager syncStateManager, SharedPreferences sharedPreferences) {
        mContext = context;
        mContentUri = contentUri;
        mAction = action;
        mResult = new ApiSyncResult(mContentUri);
        mIsUi = isUI;

        mResultKey = PREFIX_LAST_SYNC_RESULT + mContentUri.toString();
        mSyncStateManager = syncStateManager;
        mApiSyncerFactory = apiSyncerFactory;
        mSharedPreferences = sharedPreferences;
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
        Wrapper.setBackgroundMode(!mIsUi);

        /**
         *  if we have synced this uri within a certain amount of time, send a silent exception as this might be a
            symptom of a sync loop (battery/data drain)
         */
        if (System.currentTimeMillis() - localCollection.last_sync_attempt < SYNC_REPEAT_TOLERANCE){
            String message = mContentUri.toString() + " " + mSharedPreferences.getString(mResultKey, PREF_VAL_NULL);
            sendRetryViolation(mContentUri.toString(), new SyncRetryViolation(message));
        }

        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "syncing " + mContentUri);
            mResult = mApiSyncerFactory.forContentUri(mContext, mContentUri).syncContent(mContentUri, mAction);
            mSyncStateManager.onSyncComplete(mResult, localCollection);

            // update shared prefs for debugging repeat syncs
            mSharedPreferences.edit().putString(mResultKey, mResult.success ? PREF_VAL_SUCCESS : PREF_VAL_FAILED).commit();

        } catch (CloudAPI.InvalidTokenException e) {
            handleException(e, mResultKey);
            mResult = ApiSyncResult.fromAuthException(mContentUri);
            mContext.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
        } catch (IOException e) {
            handleException(e, mResultKey);
            mResult = ApiSyncResult.fromIOException(mContentUri);
        } finally {
            // should be taken care of when thread dies, but needed for tests
            Wrapper.setBackgroundMode(false);
        }


        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "executed sync on " + this);
        }
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

    @VisibleForTesting
    protected void sendRetryViolation(String message, SyncRetryViolation syncRetryViolationException) {
        SoundCloudApplication.handleSilentException(message, syncRetryViolationException);
    }

    private void handleException(IOException e, String lastResultKey) {
        Log.e(ApiSyncService.LOG_TAG, "Problem while syncing", e);
        mSyncStateManager.updateSyncState(localCollection.getId(), LocalCollection.SyncState.IDLE);
        mSharedPreferences.edit().putString(lastResultKey, e.toString()).commit();

        /**
         * Firehose beta exceptions for sync debugging purposes. we may want to turn this off
         */
        if (SoundCloudApplication.BETA_MODE && mIsUi){
            SoundCloudApplication.handleSilentException("Problem while syncing", e);
        }
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

    public class SyncRetryViolation extends Exception {
        public SyncRetryViolation(String message) {
            super(message);
        }
    }
}
