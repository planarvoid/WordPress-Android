package com.soundcloud.android.main;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleBinder;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ScreenTracker extends ActivityLightCycleDispatcher<AppCompatActivity> {
    private final EventBus eventBus;

    @LightCycle final ScreenStateProvider screenStateProvider;

    @Inject
    public ScreenTracker(ScreenStateProvider screenStateProvider, EventBus eventBus) {
        this.screenStateProvider = screenStateProvider;
        this.eventBus = eventBus;
        LightCycleBinder.bind(this);
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        super.onResume(activity);

        if (isEnteringScreen()) {
            final Screen screen = getScreen(activity);
            if (screen != Screen.UNKNOWN) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(screen));
            }
        }
    }

    private Screen getScreen(AppCompatActivity activity) {
        checkState(activity instanceof RootActivity, "ScreenTrackingController required an RootActivity");
        return ((RootActivity) activity).getScreen();
    }

    public boolean isEnteringScreen() {
        // What does it mean ? Is there a bug here ? #2664
        return !screenStateProvider.isConfigurationChange() || screenStateProvider.isReallyResuming();
    }

}
