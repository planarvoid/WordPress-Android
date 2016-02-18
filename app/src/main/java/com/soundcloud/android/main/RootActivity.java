package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsConnector;
import com.soundcloud.android.image.ImageOperationsController;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleAppCompatActivity;

import javax.inject.Inject;

public abstract class RootActivity extends LightCycleAppCompatActivity {

    @Inject @LightCycle ActivityLifeCyclePublisher lifeCyclePublisher;
    @Inject @LightCycle AnalyticsConnector analyticsConnector;
    @Inject @LightCycle ImageOperationsController imageOperationsController;
    @Inject @LightCycle protected ScreenTracker screenTracker;
    @Inject @LightCycle ForegroundController foregroundController;

    public RootActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    abstract public Screen getScreen();

}
