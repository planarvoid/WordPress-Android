
package com.soundcloud.android.adapter;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.LoadActivitiesTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapterWrapper extends RemoteCollectionAdapter {
    private boolean mVisible;
    private long mSetLastSeenTo = -1;
    private Activities mActivities = Activities.EMPTY;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
        mAutoAppend = mLocalCollection.last_sync > 0; // never synced, wait on items to allow appending to prevent premature empty view
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVisible = true;
        // update last seen for notifications
        if (mSetLastSeenTo != -1){
            setLastSeen(mSetLastSeenTo);
            mSetLastSeenTo = -1;
        }
    }

    @Override
    public Class<?> getRefreshModel() {
        return Track.class;
    }

    @Override
    public void resetData(){
        super.resetData();
        mActivities = Activities.EMPTY;
    }

    @Override
    protected Object getDataState(){
        return mActivities;
    }

    @Override
    protected void setData(Object data){
        mActivities = (Activities) data;
        getWrappedAdapter().setData(new ArrayList<Parcelable>());
        getWrappedAdapter().getData().addAll(mActivities.collection);
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

    public boolean onNewEvents(Activities newActivities, boolean wasRefresh, boolean noMoreLocalItems) {
        Log.d(getClass().getSimpleName(),"Task delivered "+ newActivities.size() + " new activities");
        if (wasRefresh) {
            setListLastUpdated();
            doneRefreshing();

        } else {
            if (noMoreLocalItems){
                mActivity.startService(new Intent(mActivity, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setAction(ApiSyncService.ACTION_APPEND)
                        .setData(mContent.uri));
            } else {
                mState = IDLE;
            }
        }

        if (!newActivities.isEmpty()){
            if (getData().isEmpty()){
                setLastSeen(newActivities.get(0).created_at.getTime());
            }
            checkForStaleItems(newActivities.collection);
            newActivities.mergeAndSort(mActivities);
            setData(newActivities);
        }

        mAppendTask = null;
        applyEmptyView();
        notifyDataSetChanged();
        return true;
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
    protected AsyncTask<Object, List<? super Parcelable>, Boolean> buildTask() {
        return new LoadActivitiesTask(mActivity.getApp(), this);
    }

    @Override
    protected Object getTaskParams(final boolean refresh){
        return new LoadActivitiesTask.ActivitiesParams() {{
            contentUri = getContentUri(refresh);
            isRefresh = refresh;
            timestamp = refresh ? mActivities.getTimestamp() : mActivities.getLastTimestamp();
            maxToLoad = Consts.COLLECTION_PAGE_SIZE;
        }};
    }

    protected void onContentChanged() {
        if (!mAutoAppend) { // result of initial sync, appending is ok now
            allowInitialLoading();
            notifyDataSetChanged();
        } else {
            if (!getData().isEmpty()){ // only refresh if we have items, otherwise appending will take care of the additions
                executeRefreshTask();
            }
            mKeepGoing = true; // make sure we are free to append
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {

                if (!mAutoAppend) {
                    allowInitialLoading();
                    notifyDataSetChanged();
                }
                if (CloudUtils.isTaskFinished(mRefreshTask)) doneRefreshing(); // no refresh task so no need to show the refresh header
                break;
            }
            case ApiSyncService.STATUS_APPEND_ERROR:
            case ApiSyncService.STATUS_APPEND_FINISHED: {

                if (!resultData.getBoolean(mContentUri.toString())) mKeepGoing = false; // no items to append, so don't keep going

                // this conditional prevent double appends that may be caused by the initial sync
                if (mAppendTask == null) {
                    mState = IDLE;
                    notifyDataSetChanged();
                }
                break;
            }
        }
    }
}
