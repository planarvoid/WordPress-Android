package com.soundcloud.android.analytics;

import com.soundcloud.android.configuration.ForceUpdateEvent;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.TrackingEvent;

import android.content.Context;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class DefaultAnalyticsProvider implements AnalyticsProvider {

    @Override
    public void flush() {

    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {

    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {

    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    @Override
    public void handleAdRichMediaSessionEvent(AdRichMediaSessionEvent eventData) {

    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {

    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {

    }

    @Override
    public void handleForceUpdateEvent(ForceUpdateEvent event) {

    }

    @Override
    public void handlePerformanceEvent(PerformanceEvent event) {

    }

    @Override
    public void onAppCreated(Context context) {

    }
}
