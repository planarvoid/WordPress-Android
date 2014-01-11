package com.soundcloud.android.analytics;


import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Scheduler;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTrackingTest {
    @SuppressWarnings("unused") // it repsonds to tracking events via Rx, but must be alive to do so
    private AnalyticsEngine analyticsEngine;
    @Mock
    private AnalyticsProperties analyticsProperties;
    @Mock
    private AnalyticsProvider analyticsProviderOne;
    @Mock
    private AnalyticsProvider analyticsProviderTwo;
    @Mock
    private AnalyticsEngine.PlaybackServiceStateWrapper playbackWrapper;
    @Mock
    private Scheduler scheduler;

    @After
    public void tearDown(){
        AnalyticsEngine.ACTIVITY_SESSION_OPEN.set(false);
    }

    @Test
    public void shouldTrackScreenForAllProvidersWhenScreenEnteredAndSessionIsOpen() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne).trackScreen(eq("screen"));
        verify(analyticsProviderTwo).trackScreen(eq("screen"));
    }

    @Test
    public void shouldNotTrackScreenWhenActivityCreatedAndAnalyticsDisabled() {
        setAnalyticsDisabled();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, never()).trackScreen(anyString());
        verify(analyticsProviderTwo, never()).trackScreen(anyString());
    }

    @Test
    public void shouldNotTrackScreenIfNoSessionIsOpen() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, never()).trackScreen(anyString());
        verify(analyticsProviderTwo, never()).trackScreen(anyString());
    }

    @Test
    public void shouldOnlySubscribeToEnterScreenEventOncePerSession() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, times(1)).trackScreen(eq("screen"));
        verify(analyticsProviderTwo, times(1)).trackScreen(eq("screen"));
    }

    // this is in fact a necessary requirement since AnalyticsEngine is a singleton, and unsubscribing
    // from one Activity must not reuse event subscriptions from previous Activities
    @Test
    public void shouldResubscribeToEnterScreenEventAfterOpenCloseSessionCycle() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, times(1)).trackScreen(eq("screen"));
        verify(analyticsProviderTwo, times(1)).trackScreen(eq("screen"));
    }

    @Test
    public void shouldTrackPlaybackEvent() throws Exception {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        PlaybackEvent playbackEvent = PlaybackEvent.forPlay(mock(Track.class), 0, Mockito.mock(TrackSourceInfo.class));
        EventBus.PLAYBACK.publish(playbackEvent);

        verify(analyticsProviderOne, times(1)).trackPlaybackEvent(playbackEvent);
        verify(analyticsProviderTwo, times(1)).trackPlaybackEvent(playbackEvent);
    }

    @Test
    public void shouldTrackSocialEvent() throws Exception {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        SocialEvent socialEvent = SocialEvent.fromFollow("screen", 0);
        EventBus.SOCIAL.publish(socialEvent);

        verify(analyticsProviderOne, times(1)).trackSocialEvent(socialEvent);
        verify(analyticsProviderTwo, times(1)).trackSocialEvent(socialEvent);
    }

    private void setAnalyticsDisabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(analyticsProperties.isAnalyticsEnabled()).thenReturn(false);
    }

    private void setAnalyticsEnabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(analyticsProperties.isAnalyticsEnabled()).thenReturn(true);
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(analyticsProperties, playbackWrapper, scheduler,
                analyticsProviderOne, analyticsProviderTwo);
    }

}
