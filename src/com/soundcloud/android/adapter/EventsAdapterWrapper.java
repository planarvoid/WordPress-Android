
package com.soundcloud.android.adapter;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.Model;
import com.soundcloud.android.activity.LazyActivity;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    private static String TAG = "AdpEvents";

    protected String mNextEventsParams;

    public EventsAdapterWrapper(LazyActivity activity, LazyBaseAdapter wrapped, String url,
            Model loadModel, String collectionKey) {
        super(activity, wrapped, url, loadModel, collectionKey);
    }

    @Override
    public void clear() {
        mNextEventsParams = "";
        super.clear();
    }

    @Override
    public String saveExtraData() {
        return mNextEventsParams;
    }

    @Override
    public void restoreExtraData(String restore) {
        mNextEventsParams = restore;
    }

    @Override
    protected String getUrl() {
        if (CloudUtils.stringNullEmptyCheck(mNextEventsParams))
            return super.getUrl();
        else
            return super.getUrl() + mNextEventsParams;
    }

    public void onNextEventsParam(String nextEventsHref) {
        mNextEventsParams = nextEventsHref.substring(nextEventsHref.indexOf("?"));
    }

}
