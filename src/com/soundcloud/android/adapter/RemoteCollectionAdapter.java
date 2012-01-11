package com.soundcloud.android.adapter;


import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Resource;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteCollectionAdapter extends LazyEndlessAdapter {

    private Uri mSyncDataUri;
    private boolean mWaitingOnSync;


    public RemoteCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity, wrapped, contentUri, request, autoAppend);
    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);

        if (isSyncable()) {

            boolean sync = true;
            if (!userRefresh) {
                executeRefreshTask();
                sync = isStale(true);
            }

            if (sync) {
                // send an intent to update our event cache
                mWaitingOnSync = true;
                requestSync();
            }

        } else {
            executeRefreshTask();
        }

        notifyDataSetChanged();
    }

    private void executeRefreshTask(){
         mRefreshTask = buildTask();
         mRefreshTask.execute(getCollectionParams(true));
    }

    protected RemoteCollectionTask buildTask() {
        return new RemoteCollectionTask(mActivity.getApp(), this);
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        mKeepGoing = keepGoing;
        mNextHref = nextHref;

        if ((newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK) {
            addNewItems(newItems);
            mState = IDLE;
            increasePageIndex();
        } else {
            handleResponseCode(responseCode);
        }

        // configure the empty view depending on possible exceptions
        applyEmptyView();
        mPendingView = null;
        mAppendTask = null;
        notifyDataSetChanged();
    }

    public void onPostRefresh(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if (handleResponseCode(responseCode) || (newItems != null && newItems.size() > 0)) {
            reset(false);
            onPostTaskExecute(newItems,nextHref,responseCode,keepGoing);
        } else if (!mWaitingOnSync) {
            applyEmptyView();
        }

        if (!mWaitingOnSync && mListView != null) {
            mListView.onRefreshComplete(false);
            setListLastUpdated();
        }

        mRefreshTask = null;
        notifyDataSetChanged();
    }

    protected void addNewItems(List<Parcelable> newItems){
        if (newItems == null || newItems.size() == 0)  return;
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(newItem);
        }
        checkForStaleItems(newItems);
    }

    protected void checkForStaleItems(List<Parcelable> newItems){
        final long stale = System.currentTimeMillis() - Consts.SYNC_STALE_TIME;
        final boolean doUpdate = CloudUtils.isWifiConnected(mActivity);

        Map<Long, Resource> toUpdate = new HashMap<Long, Resource>();
        for (Parcelable newItem : newItems) {
            if (doUpdate && newItem instanceof Resource){
                Resource resource = (Resource) newItem;
                if (resource.getLastUpdated() < stale) {
                    toUpdate.put(resource.getResourceId(), resource);
                }
            }
        }

        if (toUpdate.size() > 0){
            mUpdateCollectionTask =new UpdateCollectionTask(mActivity.getApp(),getLoadModel(false));
            mUpdateCollectionTask.setAdapter(this);
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    protected void clearUpdateTask() {
        if (mUpdateCollectionTask != null && !CloudUtils.isTaskFinished(mUpdateCollectionTask)) mUpdateCollectionTask.cancel(true);
        mUpdateCollectionTask = null;
    }

    @Override
    public boolean isRefreshing() {
        return mRefreshTask != null || mWaitingOnSync == true;
    }


    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                return true;

            case HttpStatus.SC_UNAUTHORIZED:
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            default:
                Log.w(TAG, "unexpected responseCode "+responseCode);
                mState = ERROR;
            return false;
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
                executeRefreshTask();
                /*if (resultData.getBoolean(mContentUri.toString())){
                    executeRefreshTask(false);
                } else {
                    doneRefreshing();
                }*/

                break;
            }
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mWaitingOnSync = false;
                onPostRefresh(null,null, HttpStatus.SC_NO_CONTENT, false);
                break;
            }
        }
    }


}
