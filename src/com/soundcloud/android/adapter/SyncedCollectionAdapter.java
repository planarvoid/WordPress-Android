package com.soundcloud.android.adapter;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class SyncedCollectionAdapter extends LazyEndlessAdapter implements DetachableResultReceiver.Receiver {

    private boolean mWaitingOnSync;
    private DetachableResultReceiver mDetachableReceiver;
    private UpdateCollectionTask mUpdateCollectionTask;

    public SyncedCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, boolean autoAppend) {
        super(activity,wrapped,contentUri,null,autoAppend);
    }


    protected void startAppendTask(){
        mAppendTask = new SyncedCollectionTask(mActivity.getApp(), new LoadCollectionTask.CollectionParams() {
            {
                loadModel = getLoadModel(false);
                contentUri = getContentUri(false);
                pageIndex = getPageIndex(false);
            }
        });
        mAppendTask.setAdapter(this);
        mAppendTask.execute();
    }

    protected void startRefreshTask(final boolean userRefresh) {
        mRefreshTask = new SyncedCollectionTask(mActivity.getApp(), new LoadCollectionTask.CollectionParams(){
            {
                loadModel = getLoadModel(true);
                contentUri = getContentUri(true);
                pageIndex = getPageIndex(true);
                refresh = true;
            }
        });
        mRefreshTask.setAdapter(this);
        mRefreshTask.execute();
    }

    public void onPostTaskExecute(List<Parcelable> newItems, boolean keepGoing) {
        mState = keepGoing ? WAITING : DONE;
        if (newItems != null && newItems.size() > 0) {
            increasePageIndex();
            addNewItems(newItems);
        }

        // configure the empty view depending on possible exceptions
        applyEmptyView();
        mPendingView = null;
        mAppendTask = null;
        notifyDataSetChanged();
    }

    private void addNewItems(List<Parcelable> newItems){

        final long stale = System.currentTimeMillis() - Consts.SYNC_STALE_TIME;
        final boolean doUpdate = CloudUtils.isWifiConnected(mActivity);

        Map<Long, Resource> toUpdate = new HashMap<Long, Resource>();
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(newItem);

            if (doUpdate && newItem instanceof Origin){
                Resource resource = (Resource) newItem;
                if (resource.getLastUpdated() < stale) {
                    toUpdate.put(resource.getId(), resource);
                }
            }

        }

        if (toUpdate.size() > 0){
            mUpdateCollectionTask =new UpdateCollectionTask(mActivity.getApp(),getLoadModel(false));
            mUpdateCollectionTask.setAdapter(this);
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    public void onPostRefresh(List<Parcelable> newItems, boolean keepGoing) {
        if (newItems != null && newItems.size() > 0) {
            reset(false);
            addNewItems(newItems);
            increasePageIndex();

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

        mState = REFRESHING;
        boolean sync = true;

        if (!userRefresh) {
            startRefreshTask(false); // load whatever is currently cached

            final long elapsed = System.currentTimeMillis() - LocalCollection.getLastSync(mActivity.getContentResolver(), contentUri);
            if (elapsed < Consts.DEFAULT_REFRESH_MINIMUM) {
                sync = false;
                Log.i(TAG, "Skipping sync of " + contentUri + ". Elapsed since last sync (in ms) " + elapsed);
            } else {
                Log.i(TAG, "Syncing " + contentUri + ". Elapsed since last sync (in ms) " + elapsed);
            }
        }

        if (sync) {
            // send an intent to update our event cache
            mWaitingOnSync = true;
            final Intent intent = new Intent(mActivity, ApiSyncService.class);
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
            intent.setData(mContent.uri);
            mActivity.startService(intent);
        }


        notifyDataSetChanged();
    }

    @Override
    public void append() {
        mState = APPENDING;
//        checkPageForStaleItems
        startAppendTask();
    }


    private void checkPageForStaleItems(boolean refresh){
        // do we only want to auto-refresh on wifi??
        if (Content.isSyncable(getContentUri(false)) && CloudUtils.isWifiConnected(mActivity)) {
            final Intent intent = new Intent(mActivity, ApiSyncService.class);
            intent.setAction(ApiSyncService.REFRESH_PAGE_ACTION);
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
            intent.putExtra("pageIndex",getPageIndex(refresh));
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
                //checkPageForStaleItems(true);
                break;
            }
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mWaitingOnSync = false;
                mState = ERROR;
                onPostRefresh(null, false);
                break;
            }
            case ApiSyncService.STATUS_PAGE_REFRESH_ERROR:
            case ApiSyncService.STATUS_PAGE_REFRESH_FINISHED: {
               /*final int pageIndex = resultData.getInt("pageIndex", 0);
                if (pageIndex == 0) {
                    startRefreshTask(false);
                } else {
                    startAppendTask();
                }
                */
                notifyDataSetChanged();
                break;
            }
        }
    }
}
