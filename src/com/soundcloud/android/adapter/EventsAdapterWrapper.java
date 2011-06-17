
package com.soundcloud.android.adapter;

import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.api.Request;

import android.text.TextUtils;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        super(activity, wrapped, request);
    }

    @Override
    public String saveExtraData() {
        return ((EventsAdapter)getWrappedAdapter()).nextCursor;
    }

    @Override
    public void restoreExtraData(String restore) {
        ((EventsAdapter)getWrappedAdapter()).nextCursor = restore;
    }

    @Override
    protected Request getRequest(boolean refresh) {
        Request request = super.getRequest(refresh);
        Log.i("asdf","Next request 1 " + request.toString() + " " + refresh);
        if (!refresh && !TextUtils.isEmpty(((EventsAdapter)getWrappedAdapter()).nextCursor)){
            request.add("cursor", ((EventsAdapter)getWrappedAdapter()).nextCursor);
        } else if (refresh){

        }
        Log.i("asdf","Next request 2 " + request.toString());
        return request;
    }
}
