
package com.soundcloud.android.adapter;

import android.util.Log;
import android.os.Parcelable;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Request;

import android.text.TextUtils;

import java.util.ArrayList;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        super(activity, wrapped, request);
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    @Override
    protected Request getRequest(boolean refresh) {
        Request request = super.getRequest(refresh);
        if (!refresh && !TextUtils.isEmpty(((EventsAdapter)getWrappedAdapter()).nextCursor)){
            request.add("cursor", ((EventsAdapter)getWrappedAdapter()).nextCursor);
        } else if (refresh){

        }
        return request;
    }

    @Override
    public void onPostTaskExecute(ArrayList<Parcelable> newItems, String nextHref, int responseCode, Boolean keepgoing) {
        if (newItems != null && newItems.size() > 0 &&
                mActivity.getApp().getAccountDataLong(User.DataKeys.LAST_INCOMING_SYNC_EVENT_TIMESTAMP) < ((Event) newItems.get(0)).created_at.getTime()) {
            mActivity.getApp().setAccountData(User.DataKeys.LAST_INCOMING_SYNC_EVENT_TIMESTAMP, ((Event) newItems.get(0)).created_at.getTime());
        }

        super.onPostTaskExecute(newItems,nextHref,responseCode,keepgoing);
    }
}
