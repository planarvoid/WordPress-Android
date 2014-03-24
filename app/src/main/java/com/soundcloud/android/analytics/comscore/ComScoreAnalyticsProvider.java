package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;

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
    public void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event) {
    }

    @Override
    public void handleScreenEvent(String screenTag) {}

    @Override
    public void handlePlaybackEvent(PlaybackEvent eventData) {}

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

    @Override
    public void handleUIEvent(UIEvent event) {}

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {}

    @Override
    public void handleSearchEvent(SearchEvent searchEvent) {}

}
