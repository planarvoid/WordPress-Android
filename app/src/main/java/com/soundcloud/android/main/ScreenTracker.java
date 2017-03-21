package com.soundcloud.android.main;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import rx.subjects.BehaviorSubject;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class ScreenTracker extends ActivityLightCycleDispatcher<RootActivity>
        implements EnterScreenDispatcher.Listener {
    private static final String EXTRA_HAS_TRACKED = "hasTrackedForeground";

    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;
    private final EventTracker eventTracker;
    final ReferringEventProvider referringEventProvider;

    final BehaviorSubject<Void> enterScreen = BehaviorSubject.create();

    @Inject
    public ScreenTracker(ReferringEventProvider referringEventProvider,
                         EnterScreenDispatcher enterScreenDispatcher,
                         EventTracker eventTracker) {
        this.referringEventProvider = referringEventProvider;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.eventTracker = eventTracker;

        enterScreenDispatcher.setListener(this);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        final Screen screen = activity.getScreen();
        if (screen != Screen.UNKNOWN) {
            eventTracker.trackScreen(ScreenEvent.create(screen), referringEventProvider.getReferringEvent());
        }
        enterScreen.onNext(null);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        trackForegroundEvent(activity.getIntent());
        referringEventProvider.setupReferringEvent();
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);
        trackForegroundEvent(intent);
    }

    @Override
    public void onRestoreInstanceState(RootActivity activity, Bundle bundle) {
        super.onRestoreInstanceState(activity, bundle);
        referringEventProvider.restoreReferringEvent(bundle);
    }

    @Override
    public void onSaveInstanceState(RootActivity activity, Bundle bundle) {
        super.onSaveInstanceState(activity, bundle);
        referringEventProvider.saveReferringEvent(bundle);
    }

    private void trackForegroundEvent(Intent intent) {
        if (!intent.getBooleanExtra(EXTRA_HAS_TRACKED,
                                    false) && Referrer.hasReferrer(intent) && Screen.hasScreen(intent)) {
            eventTracker.trackForegroundEvent(ForegroundEvent.open(Screen.fromIntent(intent),
                                                                   Referrer.fromIntent(intent)));
            intent.putExtra(EXTRA_HAS_TRACKED, true);
        }
    }
}
