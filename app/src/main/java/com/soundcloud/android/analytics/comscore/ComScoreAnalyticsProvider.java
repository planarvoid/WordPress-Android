package com.soundcloud.android.analytics.comscore;

import com.comscore.analytics.comScore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;

import android.content.Context;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
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
    public void handleScreenEvent(String screenTag) {}

    @Override
    public void handlePlaybackSessionEvent(PlaybackSessionEvent eventData) {}

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {}

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {}

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

    @Override
    public void handleUIEvent(UIEvent event) {}

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {}

    @Override
    public void handleSearchEvent(SearchEvent searchEvent) {}

}
