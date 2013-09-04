package com.soundcloud.android.analytics;

import android.content.Context;
import com.localytics.android.LocalyticsSession;

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
        mLocalyticsSession.upload();
    }

    @Override
    public void closeSession() {
        mLocalyticsSession.close();
        mLocalyticsSession.upload();
    }

}
