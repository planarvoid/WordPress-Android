package com.soundcloud.android.campaigns;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.R;
import com.soundcloud.android.main.DefaultActivityLifeCycle;
import com.soundcloud.android.main.ScActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;


/**
 * http://support.localytics.com/Android
 */
public class InAppCampaignController extends DefaultActivityLifeCycle<ScActivity> {

    private LocalyticsAmpSession localyticsAmpSession;
    private FragmentActivity owner;

    @Inject
    public InAppCampaignController(LocalyticsAmpSession session) {
        this.localyticsAmpSession = session;
    }

    @Override
    public void onBind(ScActivity owner) {
        this.owner = owner;
    }

    @Override
    public void onCreate(Bundle bundle) {
        final Intent intent = owner.getIntent();
        this.localyticsAmpSession.registerPush(owner.getString(R.string.google_api_key));
        this.localyticsAmpSession.handlePushReceived(intent);           // Only needed if using Localytics Push
        this.localyticsAmpSession.open();
        this.localyticsAmpSession.upload();
        this.localyticsAmpSession.handleIntent(intent);
    }

    @Override
    public void onResume() {
        final Intent intent = owner.getIntent();
        localyticsAmpSession.open();
        localyticsAmpSession.attach(owner);
        localyticsAmpSession.handleIntent(intent);
        localyticsAmpSession.handlePushReceived(intent);
    }

    @Override
    public void onPause() {
        localyticsAmpSession.detach();
        localyticsAmpSession.close();
    }
}
