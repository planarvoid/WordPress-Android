package com.soundcloud.android.analytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.PlaybackEventData;

import android.content.Context;

class LocalyticsAnalyticsProvider implements AnalyticsProvider {
    private LocalyticsSession mLocalyticsSession;
    public LocalyticsAnalyticsProvider(Context context){
        this(new LocalyticsSession(context.getApplicationContext(),
                new AnalyticsProperties(context.getResources()).getLocalyticsAppKey()));
    }

    protected LocalyticsAnalyticsProvider(LocalyticsSession localyticsSession){
        mLocalyticsSession = localyticsSession;
    }

    @Override
    public void openSession() {
        mLocalyticsSession.open();
    }

    @Override
    public void closeSession() {
        mLocalyticsSession.close();
        mLocalyticsSession.upload();
    }

    @Override
    public void flush() {
        mLocalyticsSession.upload();
    }

    @Override
    public void trackScreen(String screenTag) {
        mLocalyticsSession.tagScreen(screenTag);
    }

    @Override
    public void trackPlaybackEvent(PlaybackEventData eventData) {
        // no-op
    }

}
