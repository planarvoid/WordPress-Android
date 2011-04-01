
package com.soundcloud.android.adapter;

import android.text.TextUtils;
import com.soundcloud.android.activity.ScActivity;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
    private String mNextEventsParams;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, String url,
            Class<?> loadModel, String collectionKey) {
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
        if (TextUtils.isEmpty(mNextEventsParams)) {
            return super.getUrl();
        } else {
            return super.getUrl() + mNextEventsParams;
        }
    }

    public void onNextEventsParam(String nextEventsHref) {
        mNextEventsParams = nextEventsHref.substring(nextEventsHref.indexOf("?"));
    }
}
