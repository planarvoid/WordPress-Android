
package com.soundcloud.android.adapter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.LoadActivitiesTask;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EventsAdapterWrapper extends RemoteCollectionAdapter {
    private boolean mVisible;
    private long mSetLastSeenTo;
    private Activities mActivities = Activities.EMPTY;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
        mAutoAppend = mLocalCollection.last_sync > 0;
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVisible = true;
        if (mSetLastSeenTo != -1){
            setLastSeen(mSetLastSeenTo);
            mSetLastSeenTo = -1;
        }
    }

    @Override
    public int getPageIndex(boolean isRefresh) {
        return mPageIndex;
    }

    public void onPause() {
        mVisible = false;
    }

    public void setContent(Content c){
        mContent = c;
        mContentUri = c.uri;
        mRequest = Request.to(c.remoteUri);
        setListLastUpdated();
    }

    public void executeAppendTask() {
        if (mPageIndex == -1){
            mState = APPENDING;
            requestAppend();
        } else {
            super.executeAppendTask();
        }
    }

    public void executeRefreshTask() {
        mRefreshTask = buildTask();
        mRefreshTask.execute(getCollectionParams(true));
    }

    public boolean onNewEvents(Activities newActivities, boolean wasRefresh) {

        if (wasRefresh) {
            if (!newActivities.isEmpty()) {
                if (mListView != null && mContentUri != null) setListLastUpdated();
                setLastSeen(newActivities.get(0).created_at.getTime());
            }
        } else {
            if (newActivities.size() - mActivities.size() < Consts.COLLECTION_PAGE_SIZE){
                mPageIndex = -1; // shows all
                requestAppend();
            } else {
                increasePageIndex();
                mState = IDLE;
            }
        }

        if (!newActivities.isEmpty()){
            mActivities = newActivities;
            getWrappedAdapter().setData(new ArrayList<Parcelable>());
            getWrappedAdapter().getData().addAll(mActivities.collection);
        }

        if (!isRefreshing()) doneRefreshing();
        applyEmptyView();
        mAppendTask = null;
        notifyDataSetChanged();
        return true;
    }

    private void requestAppend() {
        Intent intent = new Intent(mActivity, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                .putExtra(ApiSyncService.EXTRA_IS_UI_RESPONSE, true)
                .setAction(ApiSyncService.ACTION_APPEND)
                .setData(mContent.uri);

        mActivity.startService(intent);
    }

    private void setLastSeen(long time) {
        final String lastSeenKey = getWrappedAdapter().isActivityFeed() ?
                User.DataKeys.LAST_OWN_SEEN : User.DataKeys.LAST_INCOMING_SEEN;

        SoundCloudApplication app = mActivity.getApp();
        final long lastSeen = app.getAccountDataLong(lastSeenKey);

        if (lastSeen < time) {
            if (mVisible) {
                mActivity.getApp().setAccountData(lastSeenKey, time);
            } else {
                mSetLastSeenTo = time;
            }
        }
    }

    @Override
    protected RemoteCollectionTask buildTask() {
        return new LoadActivitiesTask(mActivity.getApp(), this);
    }

    protected void onContentChanged() {
        if (!mAutoAppend) { // result of initial sync, appending is ok now
            allowInitialLoading();
            notifyDataSetChanged();
        } else {
            executeRefreshTask();
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                if (!resultData.getBoolean(mContentUri.toString()) && !isRefreshing()) doneRefreshing(); // nothing changed
                break;
            }
            case ApiSyncService.STATUS_APPEND_ERROR:
            case ApiSyncService.STATUS_APPEND_FINISHED: {
                if (!resultData.getBoolean(mContentUri.toString())) mKeepGoing = false;
                mState = IDLE;
                notifyDataSetChanged();
                break;
            }
        }
    }

    public Activities getActivities() {
        return mActivities;

    }
}
