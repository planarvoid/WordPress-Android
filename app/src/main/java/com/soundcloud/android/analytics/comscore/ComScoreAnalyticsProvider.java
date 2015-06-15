package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;

import android.content.Context;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class ComScoreAnalyticsProvider implements AnalyticsProvider {

    public static final int ONE_MINUTE = 60;
    public static final boolean AUTO_UPDATE_IN_BACKGROUND = false;

    public ComScoreAnalyticsProvider(Context context) {
        comScore.setAppContext(context.getApplicationContext());
        comScore.enableAutoUpdate(ONE_MINUTE, AUTO_UPDATE_IN_BACKGROUND);
    }

    @Override
    public void flush() {
        comScore.flushCache();
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
            comScore.onEnterForeground();
        } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
            comScore.onExitForeground();
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {}

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {}

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {}

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PlaybackSessionEvent) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        }
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
        // Not implemented
    }

    private void handlePlaybackSessionEvent(PlaybackSessionEvent event) {
        if (event.isPlayEvent()) {
            comScore.onUxActive();
        } else if (event.isStopEvent()) {
            comScore.onUxInactive();
        }
    }
}
