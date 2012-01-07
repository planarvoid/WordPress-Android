package com.soundcloud.android.adapter;


import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.SyncedCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncedCollectionAdapter extends LazyEndlessAdapter implements DetachableResultReceiver.Receiver {


    private UpdateCollectionTask mUpdateCollectionTask;

    public SyncedCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, boolean autoAppend) {
        super(activity, wrapped, contentUri, null, autoAppend);

        activity.getContentResolver().registerContentObserver(contentUri,false,new ContentObserver(new Handler()) {

        });
    }

    protected void startRefreshTask(){
        mRefreshTask = buildTask();
        mRefreshTask.execute(null);
    }

    protected SyncedCollectionTask buildTask(){
        SyncedCollectionTask task = new SyncedCollectionTask(mActivity.getApp(), this);
        return task;
    }

    public void onPostTaskExecute(List<Parcelable> newItems) {
        mState = (newItems.size() == Consts.COLLECTION_PAGE_SIZE) ? WAITING : DONE;
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


    /**
     * DUPLICATE FUNCTIONALITY :  see EventsAdapterWrapper.onPostRefresh
     * @param newItems
     */
    public void onPostRefresh(List<Parcelable> newItems) {
        if (newItems != null && newItems.size() > 0) {
            reset(false);
            addNewItems(newItems);
            increasePageIndex();

        } else {
            onEmptyRefresh();
        }

        if (!mWaitingOnSync) { // reset state to not refreshing
            if (mState < ERROR) mState = (newItems.size() == Consts.COLLECTION_PAGE_SIZE) ? WAITING : DONE;
            if (mListView != null) {
                mListView.onRefreshComplete(false);
                setListLastUpdated();
            }

            applyEmptyView();
            mPendingView = null;
            mRefreshTask = null;
            mAppendTask = null;
        } else {
            // this needs to be set to keep refresh state for the task started after sync returns
            mState = REFRESHING;
        }

        notifyDataSetChanged();
    }

    private void addNewItems(List<Parcelable> newItems){

        final long stale = System.currentTimeMillis() - Consts.SYNC_STALE_TIME;
        final boolean doUpdate = CloudUtils.isWifiConnected(mActivity);

        Map<Long, Resource> toUpdate = new HashMap<Long, Resource>();
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(newItem);

            if (doUpdate && newItem instanceof Resource){
                Resource resource = (Resource) newItem;
                if (resource.getLastUpdated() < stale) {
                    toUpdate.put(resource.getResourceId(), resource);
                }
            }
        }

        if (toUpdate.size() > 0){
            mUpdateCollectionTask =new UpdateCollectionTask(mActivity.getApp(),getLoadModel());
            mUpdateCollectionTask.setAdapter(this);
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);
        boolean sync = true;
        if (!userRefresh) {
            mRefreshTask = buildTask();
            mRefreshTask.execute(null);
            sync = isStale();
        }

        if (sync) {
            // send an intent to update our event cache
            mWaitingOnSync = true;
            requestSync();
        }
        notifyDataSetChanged();
    }


    private void checkPageForStaleItems(boolean refresh){
        // do we only want to auto-refresh on wifi??
        if (Content.isSyncable(getContentUri()) && CloudUtils.isWifiConnected(mActivity)) {
            /*final Intent intent = new Intent(mActivity, ApiSyncService.class);
            intent.setAction(ApiSyncService.REFRESH_PAGE_ACTION);
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
            intent.putExtra("pageIndex",getPageIndex());
            intent.setData(mContent.uri);
            mActivity.startService(intent);*/
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
                startRefreshTask();
                break;
            }
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mWaitingOnSync = false;
                mState = ERROR;
                onPostRefresh(null);
                break;
            }
        }
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
            //onContentChanged();
        }
    }
}
