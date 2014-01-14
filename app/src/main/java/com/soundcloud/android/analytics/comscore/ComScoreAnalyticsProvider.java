package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SocialEvent;

import android.content.Context;

public class ComScoreAnalyticsProvider implements AnalyticsProvider {

    public ComScoreAnalyticsProvider(Context context) {
        comScore.setAppContext(context.getApplicationContext());
    }

    @Override
    public void flush() {
        comScore.flushCache();
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        if (event.isResumeEvent()) {
            comScore.onEnterForeground();
        } else if (event.isPauseEvent()) {
            comScore.onExitForeground();
        }
    }

    @Override
    public void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event) {
    }

    @Override
    public void handleScreenEvent(String screenTag) {}

    @Override
    public void handlePlaybackEvent(PlaybackEvent eventData) {}

    @Override
    public void handleSocialEvent(SocialEvent event) {}

}
