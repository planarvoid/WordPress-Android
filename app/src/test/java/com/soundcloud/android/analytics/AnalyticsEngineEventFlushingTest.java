package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineEventFlushingTest {
    @SuppressWarnings("unused") // needs to stay alive since it subscribes to event queues
    private AnalyticsEngine analyticsEngine;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private AnalyticsProperties analyticsProperties;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private AnalyticsProvider analyticsProviderOne;
    @Mock
    private AnalyticsProvider analyticsProviderTwo;
    @Mock
    private Scheduler scheduler;
    @Mock
    private Scheduler.Worker schedulerWorker;
    @Mock
    private Subscription flushSubscription;
    @Captor
    private ArgumentCaptor<Action0> flushAction;


    @Before
    public void setUp() throws Exception {
        when(scheduler.createWorker()).thenReturn(schedulerWorker);
        when(schedulerWorker.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(flushSubscription);

        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, analyticsProperties, scheduler,
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo));
    }

    @Test
    public void shouldScheduleToFlushEventDataOnFirstTrackingEvent() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen");

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void flushActionShouldCallFlushOnAllProviders() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen");

        verify(schedulerWorker).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call();
        verify(analyticsProviderOne).flush();
        verify(analyticsProviderTwo).flush();
    }

    @Test
    public void successfulFlushShouldResetSubscription() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen");

        verify(schedulerWorker).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call();
        verify(flushSubscription).unsubscribe();
    }

    @Test
    public void shouldRescheduleFlushForTrackingEventAfterPreviousFlushFinished() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen1");

        InOrder inOrder = inOrder(schedulerWorker);

        inOrder.verify(schedulerWorker).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(); // finishes the first flush

        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen2");
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen3");
        inOrder.verify(schedulerWorker).schedule(any(Action0.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldNotScheduleFlushIfFlushIsAlreadyScheduled() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen1");
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen2");

        verify(schedulerWorker, times(1)).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromOpenSessionEvents() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromCloseSessionEvents() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromPlaybackEvents() {
        eventBus.publish(EventQueue.PLAYBACK_SESSION, PlaybackSessionEvent.forPlay(Urn.forTrack(1L), Urn.forUser(2L), mock(TrackSourceInfo.class), 123L));
        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromUIEvents() {
        eventBus.publish(EventQueue.UI, UIEvent.fromComment("screen", 1L));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }
}
