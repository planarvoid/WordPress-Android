package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.R;
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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class ComScoreAnalyticsProvider implements AnalyticsProvider {

    public static final int ONE_MINUTE = 60;
    public static final boolean AUTO_UPDATE_IN_BACKGROUND = false;

    private final Executor executor = Executors.newSingleThreadExecutor();

    public ComScoreAnalyticsProvider(Context context) {
        comScore.setAppContext(context.getApplicationContext());
        comScore.setCustomerC2(context.getString(R.string.comscore_c2));
        comScore.setPublisherSecret(context.getString(R.string.comscore_secret));
        comScore.enableAutoUpdate(ONE_MINUTE, AUTO_UPDATE_IN_BACKGROUND);
    }

    @Override
    public void flush() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                comScore.flushCache();
            }
        });
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void onAppCreated(Context context) {
        /* no op */
    }

    @Override
    public void handleActivityLifeCycleEvent(final ActivityLifeCycleEvent event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
                    comScore.onEnterForeground();
                } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
                    comScore.onExitForeground();
                }
            }
        });
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
    }

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

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (event.isPlayEvent()) {
                    comScore.onUxActive();
                } else if (event.isStopEvent()) {
                    comScore.onUxInactive();
                }
            }
        });
    }
}
