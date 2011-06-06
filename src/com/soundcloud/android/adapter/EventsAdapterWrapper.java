
package com.soundcloud.android.adapter;

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
    protected Request getRequest() {
        Request request = super.getRequest();
        if (!TextUtils.isEmpty(((EventsAdapter)getWrappedAdapter()).nextCursor)){
            request.add("cursor", ((EventsAdapter)getWrappedAdapter()).nextCursor);
        }
        return request;
    }
}
