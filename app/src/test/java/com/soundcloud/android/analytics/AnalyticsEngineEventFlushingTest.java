package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;

import android.app.Activity;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineEventFlushingTest {
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
    @Mock
    private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        when(scheduler.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(subscription);
    }

    @After
    public void tearDown(){
        AnalyticsEngine.ACTIVITY_SESSION_OPEN.set(false);
    }

    @Test
    public void shouldScheduleToFlushEventDataOnFirstTrackingEvent() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen");

        verify(scheduler).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void flushActionShouldCallFlushOnAllProviders() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen");

        ArgumentCaptor<Action0> flushAction = ArgumentCaptor.forClass(Action0.class);
        verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call();
        verify(analyticsProviderOne).flush();
        verify(analyticsProviderTwo).flush();
    }

    @Test
    public void successfulFlushShouldResetSubscription() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen");

        ArgumentCaptor<Action0> flushAction = ArgumentCaptor.forClass(Action0.class);
        verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldRescheduleFlushForTrackingEventAfterPreviousFlushFinished() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen1");

        InOrder inOrder = inOrder(scheduler, scheduler);

        ArgumentCaptor<Action0> flushAction = ArgumentCaptor.forClass(Action0.class);
        inOrder.verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(); // finishes the first flush

        Event.SCREEN_ENTERED.publish("screen2");
        inOrder.verify(scheduler).schedule(any(Action0.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldNotScheduleFlushIfFlushIsAlreadyScheduled() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen1");
        Event.SCREEN_ENTERED.publish("screen2");

        verify(scheduler, times(1)).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldNotFlushIfAnalyticsDisabled() {
        setAnalyticsDisabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.SCREEN_ENTERED.publish("screen");

        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldScheduleFlushesFromOpenSessionEvents() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(scheduler).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromCloseSessionEvents() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verify(scheduler).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromPlaybackEvents() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.PLAYBACK.publish(PlaybackEventData.forPlay(new Track(), 1L, mock(TrackSourceInfo.class)));

        verify(scheduler).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromSocialEvents() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.SOCIAL.publish(SocialEvent.fromComment("screen", 1L));

        verify(scheduler).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
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
