package com.soundcloud.android.adapter;


import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class RemoteCollectionAdapter extends LazyEndlessAdapter implements LocalCollection.OnChangeListener {

    private DetachableResultReceiver mDetachableReceiver;
    private Boolean mIsSyncable;
    protected LocalCollection mLocalCollection;
    private ChangeObserver mChangeObserver;
    private boolean mContentInvalid, mObservingContent;

    protected String mNextHref;

    public RemoteCollectionAdapter(ScListActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity, wrapped, contentUri, request, autoAppend);

        if (contentUri != null) {
            // TODO :  Move off the UI thread.
            mLocalCollection = LocalCollection.fromContentUri(contentUri, activity.getContentResolver(), true);
            mLocalCollection.startObservingSelf(activity.getContentResolver(), this);
            mChangeObserver = new ChangeObserver();
            mObservingContent = true;
            activity.getContentResolver().registerContentObserver(contentUri, true, mChangeObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSyncData();
    }

    private void refreshSyncData() {
        if (isSyncable()) {
            setListLastUpdated();

            if ((mContent != null) && mLocalCollection.shouldAutoRefresh() && !isRefreshing()) {
                refresh(false);
                // TODO : Causes loop with stale collection and server error
                // this is to show the user something at the initial load
                if (mLocalCollection.hasSyncedBefore()) mListView.setRefreshing();
            }
        }
    }

    @Override
    public Object[] saveExtraData() {
        return new Object[]{
                mNextHref,
                mContentInvalid ? 1 : 0,
                saveResultReceiver()
        };
    }

    @Override
    public void restoreExtraData(Object[] state) {
        mNextHref = (String) state[0];
        mContentInvalid = Integer.parseInt(String.valueOf(state[1])) == 1;
        if (state[2] != null) {
            restoreResultReceiver((DetachableResultReceiver) state[2]);
        }
    }

    public Class<?> getRefreshModel() {
        return getWrappedAdapter().getLoadModel();
    }

    @Override
    protected Request getRequest(boolean isRefresh) {
        if (mRequest == null) return null;
        return !(isRefresh) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : new Request(mRequest);
    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);
        if (isSyncable()) {
            requestSync();
        } else {
            clearAppendTask();
            executeRefreshTask();
            notifyDataSetChanged();
        }
    }

    @Override
    public void reset() {
        super.reset();
        mContentInvalid = false;
        mNextHref = "";
    }

    @Override
    protected boolean canShowEmptyView() {
        return (!isSyncable() || mLocalCollection.hasSyncedBefore()) && super.canShowEmptyView();
    }

    protected void setNextHref(String nextHref) {
        mNextHref = nextHref;
    }

    public boolean onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        boolean success = (newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK;
        if (success) {
            if (wasRefresh) {
                reset();
                if (mListView != null && mContentUri != null) setListLastUpdated();
            }
            setNextHref(nextHref);
            addNewItems(newItems);
            mState = IDLE;
            increasePageIndex();
        } else {
            handleResponseCode(responseCode);
        }
        mKeepGoing = keepGoing;

        if (wasRefresh && (getData().size() > 0 || !isRefreshing())) {
            doneRefreshing();
        }

        mPendingView = null;
        mAppendTask = null;
        applyEmptyView();
        notifyDataSetChanged();
        return success;
    }

    protected void addNewItems(List<Parcelable> newItems) {
        if (newItems == null || newItems.size() == 0) return;
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(newItem);
        }
        checkForStaleItems(newItems);
    }

    public void setListLastUpdated() {
        if (mListView != null) {
            if (mLocalCollection.hasSyncedBefore()) mListView.setLastUpdated(mLocalCollection.last_sync_success);
        }
    }

    protected void checkForStaleItems(List<? extends Parcelable> newItems) {
        if (!(IOUtils.isWifiConnected(mActivity)) || newItems == null || newItems.size() == 0 || !(newItems.get(0) instanceof Refreshable))
            return;

        Map<Long, ScModel> toUpdate = new HashMap<Long, ScModel>();
        for (Parcelable newItem : newItems) {
            if (newItem instanceof Refreshable) {
                ScModel resource = ((Refreshable) newItem).getRefreshableResource();
                if (resource != null) {
                    if (((Refreshable) newItem).isStale()) {
                        toUpdate.put(resource.id, resource);
                    }
                }
            }
        }

        if (toUpdate.size() > 0) {
            mUpdateCollectionTask = new UpdateCollectionTask(mActivity.getApp(), getRefreshModel());
            mUpdateCollectionTask.setAdapter(this);
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    protected void clearUpdateTask() {
        if (mUpdateCollectionTask != null && !AndroidUtils.isTaskFinished(mUpdateCollectionTask))
            mUpdateCollectionTask.cancel(true);
        mUpdateCollectionTask = null;
    }

    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                return true;

            case HttpStatus.SC_UNAUTHORIZED:
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
                //noinspection fallthrough
            default:
                Log.w(TAG, "unexpected responseCode " + responseCode);
                mState = ERROR;
                return false;
        }
    }

    @Override
    protected AsyncTask<Object, List<? super Parcelable>, Boolean> buildTask() {
        return new RemoteCollectionTask(mActivity.getApp(), this);
    }

    @Override
    protected Object getTaskParams(final boolean refresh) {
        return new RemoteCollectionTask.CollectionParams() {{
            loadModel = getLoadModel(refresh);
            contentUri = getContentUri(refresh);
            request = buildRequest(refresh);
            isRefresh = refresh;
            refreshPageItems = !isSyncable();
            startIndex = refresh ? 0 : getData().size();
            maxToLoad = Consts.COLLECTION_PAGE_SIZE;
        }};
    }

    protected boolean isSyncable() {
        if (mIsSyncable == null) {
            mIsSyncable = mContent != null && mContent.isSyncable();
        }
        return mIsSyncable;
    }

    protected DetachableResultReceiver getReceiver() {
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    public void restoreResultReceiver(DetachableResultReceiver receiver) {
        mDetachableReceiver = receiver;
        mDetachableReceiver.setReceiver(this);

    }

    public DetachableResultReceiver saveResultReceiver() {
        if (mDetachableReceiver != null) mDetachableReceiver.clearReceiver();
        return mDetachableReceiver;
    }

    public Uri getPlayableUri() {
        return mContentInvalid ? null : super.getPlayableUri();
    }

    protected void requestSync() {
        Intent intent = new Intent(mActivity, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(mContent.uri);
        mActivity.startService(intent);
    }

    @Override
    public boolean isRefreshing() {
        if (mLocalCollection != null) {
            return mLocalCollection.sync_state == LocalCollection.SyncState.SYNCING
                    || mLocalCollection.sync_state == LocalCollection.SyncState.PENDING
                    || super.isRefreshing();
        } else {
            return super.isRefreshing();
        }
    }

    protected void doneRefreshing() {
        if (isSyncable()) setListLastUpdated();
        if (mListView != null) mListView.onRefreshComplete();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                if (mContentUri != null && resultData != null &&
                        !resultData.getBoolean(mContentUri.toString()) && !isRefreshing()) {
                    doneRefreshing(); // nothing changed
                }
                break;
            }
        }
    }

    private void stopObservingChanges() {
        if (mObservingContent) {
            mObservingContent = false;

            if (mChangeObserver != null) {
                mActivity.getContentResolver().unregisterContentObserver(mChangeObserver);
            }
            if (mLocalCollection != null) {
                mLocalCollection.stopObservingSelf();
            }
        }
    }

    public void onDestroy() {
        stopObservingChanges();
    }

    @Override
    public void onLogout() {
        stopObservingChanges();
    }

    protected void onContentChanged() {
        mContentInvalid = true;
        executeRefreshTask();
    }

    @Override
    public void onLocalCollectionChanged() {
        refreshSyncData();
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            // even after unregistering, we will still get asynchronous change notifications, so make sure we want them
            if (mObservingContent) onContentChanged();
        }
    }
}
