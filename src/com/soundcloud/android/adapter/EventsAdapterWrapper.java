
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.api.Request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.text.TextUtils;

import java.net.URI;
import java.util.List;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    private String mNextEventsCursor;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, String url,
            Class<?> loadModel, String collectionKey) {
        super(activity, wrapped, url, loadModel, collectionKey);
    }

    @Override
    public void clear() {
        mNextEventsCursor = "";
        super.clear();
    }

    @Override
    public String saveExtraData() {
        return mNextEventsCursor;
    }

    @Override
    public void restoreExtraData(String restore) {
        mNextEventsCursor = restore;
    }

    @Override
    protected Request getRequest() {
        Request request = super.getRequest();
        request.add("cursor", (TextUtils.isEmpty(mNextEventsCursor))
                    ? ((EventsAdapter)getWrappedAdapter()).getNextCursor()
                            : mNextEventsCursor);
        return request;
    }

    public void onNextEventsParam(String nextEventsHref) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(nextEventsHref),"UTF-8");
        for (NameValuePair param : params){
            if (param.getName().equalsIgnoreCase("cursor")) mNextEventsCursor = param.getValue();
        }
    }
}
