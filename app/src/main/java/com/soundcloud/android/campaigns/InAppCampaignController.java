package com.soundcloud.android.campaigns;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.R;
import com.soundcloud.android.main.DefaultActivityLifeCycle;
import com.soundcloud.android.main.ScActivity;

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
        localyticsAmpSession.registerPush(owner.getString(R.string.google_api_key));
    }

    @Override
    public void onResume() {
        localyticsAmpSession.open();
        localyticsAmpSession.attach(owner);
        localyticsAmpSession.handleIntent(owner.getIntent());
        localyticsAmpSession.handlePushReceived(owner.getIntent());
    }

    @Override
    public void onPause() {
        localyticsAmpSession.detach();
        localyticsAmpSession.close();
    }
}
