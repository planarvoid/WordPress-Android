
package com.soundcloud.android.adapter;

import android.content.Intent;

import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.RefreshEventsTask;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    public DetachableResultReceiver mReceiver;
    private boolean mWaitingOnSync;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    public void setContent(Content c){
        mContent = c.uri;
        mRequest = Request.to(c.remoteUri);
        setListLastUpdated();
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

    @SuppressWarnings("unchecked")
    public void refresh(final boolean userRefresh) {
        mState = REFRESHING;
        boolean sync = true;

        if (!userRefresh) {
            startRefreshTask(false); // load whatever is currently cached

            final long elapsed = System.currentTimeMillis() - LocalCollection.getLastSync(mActivity.getContentResolver(), mContent);
            if (elapsed < Consts.DEFAULT_REFRESH_MINIMUM) {
                sync = false;
                Log.i(TAG, "Skipping sync of " + mContent + ". Elapsed since last sync (in ms) " + elapsed);
            } else {
                Log.i(TAG, "Syncing " + mContent + ". Elapsed since last sync (in ms) " + elapsed);
            }
        }

        if (sync) {
            // send an intent to update our event cache
            mWaitingOnSync = true;
            final Intent intent = new Intent(mActivity, ApiSyncService.class);
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
            intent.setData(mContent);
            mActivity.startService(intent);
        }
        notifyDataSetChanged();
    }

    @Override
    protected void startRefreshTask(final boolean userRefresh){
       mRefreshTask = new RefreshEventsTask(mActivity.getApp(), new LoadCollectionTask.Params()) {
            {
                setAdapter(EventsAdapterWrapper.this);
                cacheFile = ActivitiesCache.getCacheFile(mActivity.getApp(),mRequest);
                execute();
            }
        };
    }

    @Override
    protected LoadCollectionTask.Params buildAppendParams() {
        return new LoadCollectionTask.Params() {
            {
                loadModel = getLoadModel(false);
                pageIndex = getPageIndex(false);
                request = buildRequest(false);
            }
        };
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
                onPostRefresh(null,null,false);
                break;
            }
        }
    }

}
