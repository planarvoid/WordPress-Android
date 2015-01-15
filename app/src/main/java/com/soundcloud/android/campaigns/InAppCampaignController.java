package com.soundcloud.android.campaigns;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.R;
import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;


/**
 * http://support.localytics.com/Android
 */
public class InAppCampaignController extends DefaultActivityLightCycle {

    private final LocalyticsAmpSession localyticsAmpSession;

    @Inject
    public InAppCampaignController(LocalyticsAmpSession session) {
        this.localyticsAmpSession = session;
    }

    @Override
    public void onCreate(FragmentActivity activity, Bundle bundle) {
        final Intent intent = activity.getIntent();
        this.localyticsAmpSession.registerPush(activity.getString(R.string.google_api_key));
        this.localyticsAmpSession.handlePushReceived(intent);           // Only needed if using Localytics Push
        this.localyticsAmpSession.open();
        this.localyticsAmpSession.upload();
        this.localyticsAmpSession.handleIntent(intent);
    }

    @Override
    public void onResume(FragmentActivity activity) {
        final Intent intent = activity.getIntent();
        localyticsAmpSession.open();
        localyticsAmpSession.attach(activity);
        localyticsAmpSession.handleIntent(intent);
        localyticsAmpSession.handlePushReceived(intent);
    }

    @Override
    public void onPause(FragmentActivity activity) {
        localyticsAmpSession.detach();
        localyticsAmpSession.close();
    }
}
