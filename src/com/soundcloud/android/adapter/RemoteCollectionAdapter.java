package com.soundcloud.android.adapter;


import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Resource;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.LoadCollectionTask;
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
    private UpdateCollectionTask mUpdateCollectionTask;

    public RemoteCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity,wrapped,contentUri,request,autoAppend);
    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);

        if (isSyncable()) {

            boolean sync = true;
            if (!userRefresh) {
                executeRefreshTask(userRefresh);
                sync = isStale();
            }

            if (sync) {
                // send an intent to update our event cache
                mWaitingOnSync = true;
                requestSync();
            }

        } else {
            executeRefreshTask(userRefresh);
        }

        notifyDataSetChanged();
    }

    private void executeRefreshTask(final boolean userRefresh){
         mRefreshTask = buildTask();
         mRefreshTask.execute(getCollectionParams());
    }

    protected LoadCollectionTask buildTask() {
        return new LoadCollectionTask(mActivity.getApp(), this);
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if ((newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK) {
            mState = keepGoing ? WAITING : DONE;
            mNextHref = nextHref;
            increasePageIndex();
        } else {
            handleResponseCode(responseCode);
        }

        if (newItems != null && newItems.size() > 0) {
            addNewItems(newItems);
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
            mNextHref = nextHref;
            addNewItems(newItems);
            increasePageIndex();
        } else {
            onEmptyRefresh();
        }

        if (!mWaitingOnSync) {
            // reset state to not refreshing
            if (getWrappedAdapter().getCount() < Consts.COLLECTION_PAGE_SIZE){
                if (mState < ERROR) mState = DONE;
            } else mState = WAITING;

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
                executeRefreshTask(false);
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
