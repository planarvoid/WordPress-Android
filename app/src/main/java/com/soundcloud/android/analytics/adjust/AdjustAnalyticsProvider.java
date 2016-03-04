package com.soundcloud.android.analytics.adjust;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;

import android.content.Context;

import javax.inject.Inject;

public class AdjustAnalyticsProvider extends DefaultAnalyticsProvider {
    private final AdjustWrapper adjustWrapper;

    @Inject
    public AdjustAnalyticsProvider(AdjustWrapper adjustWrapper) {
        this.adjustWrapper = adjustWrapper;
    }

    @Override
    public void onAppCreated(Context context) {
        adjustWrapper.onCreate(context);
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
            adjustWrapper.onResume();
        } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
            adjustWrapper.onPause();
        }
    }
}
