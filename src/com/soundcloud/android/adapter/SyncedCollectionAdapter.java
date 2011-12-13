package com.soundcloud.android.adapter;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.SyncedCollectionTask;
import com.soundcloud.android.task.LoadRemoteCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class SyncedCollectionAdapter extends LazyEndlessAdapter implements DetachableResultReceiver.Receiver {

    private boolean mWaitingOnSync;
    private DetachableResultReceiver mDetachableReceiver;

    public SyncedCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, boolean autoAppend) {
        super(activity,wrapped,contentUri,null,autoAppend);
    }


    protected void startAppendTask(){
        mState = APPENDING;
        mAppendTask = new SyncedCollectionTask(mActivity.getApp(), buildAppendParams());
        mAppendTask.setAdapter(this);
        mAppendTask.execute();
    }

    protected void startRefreshTask(final boolean userRefresh) {
        mState = REFRESHING;
        mRefreshTask = new SyncedCollectionTask(mActivity.getApp(), buildRefreshParams());
        mRefreshTask.setAdapter(this);
        mRefreshTask.execute();
    }

    protected LoadCollectionTask.CollectionParams buildAppendParams() {
        return new LoadCollectionTask.CollectionParams() {
            {
                loadModel = getLoadModel(false);
                contentUri = getContentUri(false);
                pageIndex = getPageIndex(false);
            }
        };
    }

    protected LoadCollectionTask.CollectionParams buildRefreshParams(){
        return new LoadCollectionTask.CollectionParams(){
            {
                loadModel = getLoadModel(true);
                contentUri = getContentUri(true);
                pageIndex = getPageIndex(true);
                refresh = true;
            }
        };
    }

    public void onPostTaskExecute(List<Parcelable> newItems, boolean keepGoing) {
        mState = keepGoing ? WAITING : DONE;
        if (newItems != null && newItems.size() > 0) {
            checkPageForStaleItems();
            increasePageIndex();
            for (Parcelable newitem : newItems) {
                getWrappedAdapter().addItem(newitem);
            }
        }

        // configure the empty view depending on possible exceptions
        applyEmptyView();
        mPendingView = null;
        mAppendTask = null;
        notifyDataSetChanged();
    }

    public void onPostRefresh(List<Parcelable> newItems, boolean keepGoing) {
        if (newItems != null && newItems.size() > 0) {
            reset(false);
            checkPageForStaleItems();
            increasePageIndex();
            getData().addAll(newItems);
        } else {
            onEmptyRefresh();
        }

        if (!mWaitingOnSync) { // reset state to not refreshing
            if (mState < ERROR) mState = keepGoing ? WAITING : DONE;
            if (mListView != null) {
                mListView.onRefreshComplete(false);
                setListLastUpdated();
            }

            applyEmptyView();
            mPendingView = null;
            mRefreshTask = null;
            mAppendTask = null;
        }

        notifyDataSetChanged();
    }

    @Override
    public void refresh(final boolean userRefresh) {
        if (userRefresh) {
            if (FollowStatus.Listener.class.isAssignableFrom(getWrappedAdapter().getClass())) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(), (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }

        final Uri contentUri = getContentUri(true);
        if (contentUri != null && Content.isSyncable(contentUri)) {
            mState = REFRESHING;
            boolean sync = true;

            if (!userRefresh){
                startRefreshTask(false); // load whatever is currently cached

                final long elapsed = System.currentTimeMillis() - LocalCollection.getLastSync(mActivity.getContentResolver(), contentUri);
                if (elapsed < Consts.DEFAULT_REFRESH_MINIMUM){
                    sync = false;
                    Log.i(TAG, "Skipping sync of " + contentUri + ". Elapsed since last sync (in ms) " + elapsed);
                } else {
                    Log.i(TAG,"Syncing " + contentUri + ". Elapsed since last sync (in ms) " + elapsed);
                }
            }

            if (sync){
                // send an intent to update our event cache
                mWaitingOnSync = true;
                final Intent intent = new Intent(mActivity, ApiSyncService.class);
                intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
                intent.setData(mContent.uri);
                mActivity.startService(intent);
            }

        } else {
            startRefreshTask(userRefresh);
        }

        notifyDataSetChanged();
    }

    @Override
    public void append() {
        startAppendTask();
    }


    private void checkPageForStaleItems(){
        // do we only want to auto-refresh on wifi??
        if (Content.isSyncable(getContentUri(false)) && CloudUtils.isWifiConnected(mActivity)) {
            final Intent intent = new Intent(mActivity, ApiSyncService.class);
            intent.setAction(ApiSyncService.REFRESH_PAGE_ACTION);
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
            intent.putExtra("pageIndex",getPageIndex(false));
            intent.setData(mContent.uri);
            mActivity.startService(intent);
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_RUNNING: {
                break;
            }
            case ApiSyncService.STATUS_SYNC_FINISHED: {
                mWaitingOnSync = false;
                startRefreshTask(false);
                break;
            }
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mWaitingOnSync = false;
                mState = ERROR;
                onPostRefresh(null, false);
                break;
            }
            case ApiSyncService.STATUS_PAGE_REFRESH_ERROR: {
                break;
            }
            case ApiSyncService.STATUS_PAGE_REFRESH_FINISHED: {
                notifyDataSetChanged();
                break;
            }
        }
    }
}
