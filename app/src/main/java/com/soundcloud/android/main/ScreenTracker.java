package com.soundcloud.android.main;

import com.soundcloud.android.analytics.ActivityReferringEventProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;

import javax.inject.Inject;

public class ScreenTracker extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener {
    @LightCycle final ForegroundController foregroundController;
    @LightCycle final ActivityReferringEventProvider referringEventProvider;
    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;
    private final EventTracker eventTracker;

    @Inject
    public ScreenTracker(ForegroundController foregroundController,
                         ActivityReferringEventProvider referringEventProvider,
                         EnterScreenDispatcher enterScreenDispatcher,
                         EventTracker eventTracker) {
        this.foregroundController = foregroundController;
        this.referringEventProvider = referringEventProvider;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.eventTracker = eventTracker;

        this.enterScreenDispatcher.setListener(this);
        LightCycles.bind(this);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        final Screen screen = activity.getScreen();
        if (screen != Screen.UNKNOWN) {
            eventTracker.trackScreen(ScreenEvent.create(screen), referringEventProvider.getReferringEvent());
        }
    }
}
