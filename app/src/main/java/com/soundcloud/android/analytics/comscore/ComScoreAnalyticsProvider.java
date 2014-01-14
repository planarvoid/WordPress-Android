package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;

import android.content.Context;

public class ComScoreAnalyticsProvider implements AnalyticsProvider {

    public ComScoreAnalyticsProvider(Context context) {
        comScore.setAppContext(context.getApplicationContext());
    }

    @Override
    public void openSession() {
        comScore.onEnterForeground();
    }

    @Override
    public void closeSession() {
        comScore.onExitForeground();
    }

    @Override
    public void flush() {
        comScore.flushCache();
    }

    @Override
    public void handleScreenEvent(String screenTag) {}

    @Override
    public void handlePlaybackEvent(PlaybackEvent eventData) {}

    @Override
    public void handleSocialEvent(SocialEvent event) {}

}
