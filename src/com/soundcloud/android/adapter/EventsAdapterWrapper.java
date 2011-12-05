
package com.soundcloud.android.adapter;

import android.content.Intent;

import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.ApiService;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.android.task.RefreshEventsTask;
import com.soundcloud.android.utils.DetachableResultReceiver;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    public DetachableResultReceiver mReceiver;
    private int mEventType;
    private boolean mWaitingOnSync;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, int type) {
        super(activity, wrapped, Event.getRequestFromType(type), null);
        mEventType = type;
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    public void setType(int type){
        setRequest(Event.getRequestFromType(type));
    }

    @Override
    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepgoing) {
        final String lastSeenKey = getWrappedAdapter().isActivityFeed() ?
                User.DataKeys.LAST_OWN_SEEN : User.DataKeys.LAST_INCOMING_SEEN;

        if (newItems != null && !newItems.isEmpty()) {
            SoundCloudApplication app = mActivity.getApp();

            final Event first = (Event) newItems.get(0);
            final long lastSeen = app.getAccountDataLong(lastSeenKey);

            if (lastSeen < first.created_at.getTime()) {
                app.setAccountData(lastSeenKey, first.created_at.getTime());
            }
        }
        super.onPostTaskExecute(newItems,nextHref,responseCode,keepgoing);
    }

    public void onPostRefresh(List<Parcelable> newItems, String nextHref, boolean success) {

        if (success) { // apply cached items
            if (getWrappedAdapter().getCount() > 0 && newItems.contains(getData().get(0))) {
                int i = 0;
                for (Parcelable e : newItems) {
                    if (getData().contains(e)) {
                        break;
                    } else {
                        getData().add(i, e);
                        i++;
                    }
                }
            } else {
                mNextHref = nextHref;
                getData().addAll(newItems);
            }
        }

        if (!mWaitingOnSync) { // reset state to not refreshing
            if (mState < ERROR) mState = TextUtils.isEmpty(mNextHref) ? DONE : WAITING;
            if (mListView != null) {
                mListView.onRefreshComplete((newItems != null && newItems.size() > 0));
            }

            applyEmptyView();
            mPendingView = null;
            mRefreshTask = null;
            mAppendTask = null;
        }

        notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    public void refresh(final boolean userRefresh) {
        mState = REFRESHING;
        mWaitingOnSync = true;

        if (!userRefresh){
            reset(true,false);
            startRefreshTask(false); // load whatever is currently cached
        }

        // send an intent to update our event cache
        final Intent intent = new Intent(mActivity, ApiService.class);
        intent.putExtra(ApiService.EXTRA_STATUS_RECEIVER, getReceiver());
        intent.putExtra(Event.getSyncExtraFromType(mEventType),true);
        mActivity.startService(intent);

        notifyDataSetChanged();
    }

    @Override
    protected void startRefreshTask(final boolean userRefresh){
       mRefreshTask = new RefreshEventsTask(mActivity.getApp()) {
            {
                setAdapter(EventsAdapterWrapper.this);
                mCacheFile = ActivitiesCache.getCacheFile(mActivity.getApp(),mRequest);
                execute();
            }
        };
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiService.STATUS_RUNNING: {
                break;
            }
            case ApiService.STATUS_FINISHED: {
                mWaitingOnSync = false;
                startRefreshTask(false);
                break;
            }
            case ApiService.STATUS_ERROR: {
                mWaitingOnSync = false;
                mState = ERROR;
                onPostRefresh(null,null,false);
                break;
            }
        }
    }

}
