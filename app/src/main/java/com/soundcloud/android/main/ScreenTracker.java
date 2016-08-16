package com.soundcloud.android.main;

import com.soundcloud.android.analytics.ActivityReferringEventProvider;
import com.soundcloud.android.analytics.TheTracker;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;

import javax.inject.Inject;

public class ScreenTracker extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener {
    @LightCycle final ActivityReferringEventProvider referringEventProvider;
    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;
    private final TheTracker theTracker;

    @Inject
    public ScreenTracker(ActivityReferringEventProvider referringEventProvider,
                         EnterScreenDispatcher enterScreenDispatcher,
                         TheTracker theTracker) {
        this.referringEventProvider = referringEventProvider;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.theTracker = theTracker;

        this.enterScreenDispatcher.setListener(this);
        LightCycles.bind(this);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        final Screen screen = activity.getScreen();
        if (screen != Screen.UNKNOWN) {
            theTracker.trackScreen(ScreenEvent.create(screen), referringEventProvider.getReferringEvent());
        }
    }
}
