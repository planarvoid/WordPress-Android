package com.soundcloud.android.campaigns;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.R;
import com.soundcloud.android.main.DefaultActivityLifeCycle;
import com.soundcloud.android.main.ScActivity;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class InAppCampaignController extends DefaultActivityLifeCycle<ScActivity> {

    private final LocalyticsAmpSessionFactory sessionFactory;
    private LocalyticsAmpSession localyticsAmpSession;
    private FragmentActivity owner;

    @Inject
    public InAppCampaignController(LocalyticsAmpSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void onBind(ScActivity owner) {
        this.owner = owner;
        localyticsAmpSession = sessionFactory.create(owner);
        localyticsAmpSession.registerPush(owner.getString(R.string.google_api_key));
    }

    @Override
    public void onResume() {
        this.localyticsAmpSession.open();
        this.localyticsAmpSession.upload();
        this.localyticsAmpSession.attach(owner);
        this.localyticsAmpSession.handleIntent(owner.getIntent());
    }

    @Override
    public void onPause() {
        this.localyticsAmpSession.detach();
        this.localyticsAmpSession.close();
        this.localyticsAmpSession.upload();
        super.onPause();
    }

    static class LocalyticsAmpSessionFactory {

        @Inject
        public LocalyticsAmpSessionFactory() {
        }

        LocalyticsAmpSession create(FragmentActivity fragmentActivity){
            return new LocalyticsAmpSession(fragmentActivity);
        }
    }
}
