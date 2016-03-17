package com.soundcloud.android.main;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class ScreenTracker extends ActivityLightCycleDispatcher<RootActivity> {
    private final EventBus eventBus;

    @LightCycle final ScreenStateProvider screenStateProvider;

    @Inject
    public ScreenTracker(ScreenStateProvider screenStateProvider, EventBus eventBus) {
        this.screenStateProvider = screenStateProvider;
        this.eventBus = eventBus;
        LightCycles.bind(this);
    }

    @Override
    public void onResume(RootActivity activity) {
        super.onResume(activity);

        if (isEnteringScreen()) {
            final Screen screen = activity.getScreen();
            if (screen != Screen.UNKNOWN) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(screen));
            }
        }
    }

    public boolean isEnteringScreen() {
        // What does it mean ? Is there a bug here ? #2664
        return !screenStateProvider.isConfigurationChange() || screenStateProvider.isReallyResuming();
    }

}
