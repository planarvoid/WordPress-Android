package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;

import android.content.Context;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ComScoreAnalyticsProvider extends DefaultAnalyticsProvider {

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
        executor.execute(() -> comScore.flushCache());
    }

    @Override
    public void handleActivityLifeCycleEvent(final ActivityLifeCycleEvent event) {
        executor.execute(() -> {
            if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
                comScore.onEnterForeground();
            } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
                comScore.onExitForeground();
            }
        });
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PlaybackSessionEvent) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        }
    }

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        executor.execute(() -> {
            if (event.isPlayOrPlayStartEvent()) {
                comScore.onUxActive();
            } else if (event.isStopEvent()) {
                comScore.onUxInactive();
            }
        });
    }
}
