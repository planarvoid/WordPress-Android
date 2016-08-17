package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsConnector;
import com.soundcloud.android.configuration.ConfigurationUpdateLightCycle;
import com.soundcloud.android.configuration.ForceUpdateLightCycle;
import com.soundcloud.android.image.ImageOperationsController;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleAppCompatActivity;
import com.soundcloud.lightcycle.LightCycles;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public abstract class RootActivity extends LightCycleAppCompatActivity<RootActivity> {

    @Inject @LightCycle ActivityLifeCyclePublisher lifeCyclePublisher;
    @Inject @LightCycle ActivityLifeCycleLogger lifeCycleLogger;
    @Inject @LightCycle AnalyticsConnector analyticsConnector;
    @Inject @LightCycle ImageOperationsController imageOperationsController;
    @Inject @LightCycle protected ScreenTracker screenTracker;
    @Inject @LightCycle ForegroundController foregroundController;
    @Inject @LightCycle ForceUpdateLightCycle forceUpdateLightCycle;
    @Inject ConfigurationUpdateLightCycle configurationUpdateLightCycle;

    public RootActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        if (receiveConfigurationUpdates()) {
            bind(LightCycles.<AppCompatActivity, RootActivity>lift(configurationUpdateLightCycle));
        }
    }

    abstract public Screen getScreen();

    protected boolean receiveConfigurationUpdates() {
        return true;
    }
}
