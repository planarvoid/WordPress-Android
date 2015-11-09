package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsConnector;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleAppCompatActivity;

import javax.inject.Inject;

/*
 * This base class can be used for lifecycle tracking where extending from ScActivity is not necessary.
 */
public abstract class TrackedActivity extends LightCycleAppCompatActivity {

    @Inject @LightCycle ActivityLifeCyclePublisher lifeCyclePublisher;
    @Inject @LightCycle AnalyticsConnector analyticsConnector;

    public TrackedActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

}
