
package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.RefreshEventsTask;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;

public class EventsAdapterWrapper extends RemoteCollectionAdapter {
    public DetachableResultReceiver mReceiver;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    public void setContent(Content c){
        mContent = c;
        mContentUri = c.uri;
        mRequest = Request.to(c.remoteUri);
        setListLastUpdated();
    }



    @Override
    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        final String lastSeenKey = getWrappedAdapter().isActivityFeed() ?
                User.DataKeys.LAST_OWN_SEEN : User.DataKeys.LAST_INCOMING_SEEN;

        if (newItems != null && !newItems.isEmpty()) {
            SoundCloudApplication app = mActivity.getApp();

            final Activity first = (Activity) newItems.get(0);
            final long lastSeen = app.getAccountDataLong(lastSeenKey);

            if (lastSeen < first.created_at.getTime()) {
                app.setAccountData(lastSeenKey, first.created_at.getTime());
            }
        }
        super.onPostTaskExecute(newItems, nextHref, responseCode, keepGoing, wasRefresh);
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
                mKeepGoing = TextUtils.isEmpty(mNextHref);
                getData().addAll(newItems);
            }

        }


            if (mListView != null) {
                mListView.onRefreshComplete(false);
                setListLastUpdated();
            }

            // if this is the end of the initial refresh, then allow appending
            if (mState < LOADING) mState = IDLE;

            applyEmptyView();
            mPendingView = null;
            mAppendTask = null;

        notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    public void refresh(final boolean userRefresh) {
        requestSync();
        notifyDataSetChanged();
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                doneRefreshing();
                break;
            }
        }
    }

}
