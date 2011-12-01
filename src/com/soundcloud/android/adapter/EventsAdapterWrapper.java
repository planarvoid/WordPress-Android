
package com.soundcloud.android.adapter;

import android.content.Intent;
import android.net.Uri;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.ApiService;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.android.task.AppendEventsTask;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.task.RefreshEventsTask;
import com.soundcloud.android.task.RefreshTask;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.sql.Ref;
import java.util.List;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    public DetachableResultReceiver mReceiver;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Request request, Uri contentUri) {
        super(activity, wrapped, request, contentUri);
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
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

    @Override
    protected void startAppendTask(){
        mAppendTask = new AppendEventsTask(mActivity.getApp()) {
            {
                loadModel = getLoadModel(false);
                pageSize = getPageSize();
                setAdapter(EventsAdapterWrapper.this);
                request = buildRequest(false);
                mCacheFile = ActivitiesCache.getCacheFile(mActivity.getApp(),mRequest);
                refresh = false;
                execute();
            }
        };
    }

    /*
    @Override
    public void onRefresh(){
        Log.i("asdf","ON REFRESH");

    }

    @SuppressWarnings("unchecked")
    public void refresh(final boolean userRefresh) {
        if (userRefresh) {
            if (FollowStatus.Listener.class.isAssignableFrom(getWrappedAdapter().getClass())) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(), (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }

        // trigger off background sync
        final Intent intent = new Intent(mActivity, ApiService.class);
        intent.putExtra(ApiService.EXTRA_STATUS_RECEIVER, getReceiver());
        intent.putExtra(ApiService.SyncExtras.INCOMING,true);
        mActivity.startService(intent);

        startRefreshTask(userRefresh);
        notifyDataSetChanged();
    }


    @Override
    protected void startRefreshTask(final boolean userRefresh){
       mRefreshTask = new RefreshEventsTask(mActivity.getApp()) {
            {
                loadModel = getLoadModel(false);
                pageSize  = getPageSize();
                setAdapter(EventsAdapterWrapper.this);
                request = buildRequest(true);
                mCacheFile = ActivitiesCache.getCacheFile(mActivity.getApp(),mRequest);
                refresh = userRefresh;
                execute();
            }
        };
    }
      */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiService.STATUS_RUNNING: {
                Log.i("asdf","REceived result, running");
                break;
            }
            case ApiService.STATUS_FINISHED: {
                Log.i("asdf", "REceived result, finished");
                break;
            }
            case ApiService.STATUS_ERROR: {
                Log.i("asdf", "REceived result, error");
                break;
            }
        }
    }
}
