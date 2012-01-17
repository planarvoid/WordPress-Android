package com.soundcloud.android.adapter;


import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.database.ContentObserver;
import android.os.Handler;
import android.text.TextUtils;
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

    private ChangeObserver mChangeObserver;
    public RemoteCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity, wrapped, contentUri, request, autoAppend);

        if (contentUri != null){
            mChangeObserver = new ChangeObserver();
            activity.getContentResolver().registerContentObserver(contentUri, true, mChangeObserver);
        }

        if (isSyncable() && isStale(false)){
            refresh(false);
        }
    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);

        if (isSyncable()) {
            requestSync();
        } else {
            clearAppendTask();
            load(true);
        }

        notifyDataSetChanged();
    }

    protected RemoteCollectionTask buildTask() {
        return new RemoteCollectionTask(mActivity.getApp(), this);
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        mKeepGoing = keepGoing;
        mNextHref = nextHref;

        boolean success = (newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK;
        if (success) {
            if (wasRefresh){
                reset();
                if (mListView != null && mContentUri != null) setListLastUpdated();
            }
            addNewItems(newItems);
            mState = IDLE;
            increasePageIndex();
        } else {
            handleResponseCode(responseCode);
        }

        if (wasRefresh || !mRefreshing){
            doneRefreshing();
        }

        applyEmptyView();
        mPendingView = null;
        mAppendTask = null;
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
        if (!(CloudUtils.isWifiConnected(mActivity)) || newItems == null || newItems.size() == 0 || !(newItems.get(0) instanceof Resource))
            return;

        final long stale = System.currentTimeMillis() - ((Resource) newItems.get(0)).getStaleTime();

        Map<Long, Resource> toUpdate = new HashMap<Long, Resource>();
        for (Parcelable newItem : newItems) {
            if (newItem instanceof Resource){
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
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mRefreshing = false;
                if (!resultData.getBoolean(mContentUri.toString())) doneRefreshing(); // nothing changed
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
            reset();
        }
    }
}
