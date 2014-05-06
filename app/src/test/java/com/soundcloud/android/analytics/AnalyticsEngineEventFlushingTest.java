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
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineEventFlushingTest {
    @SuppressWarnings("unused") // needs to stay alive since it subscribes to event queues
    private AnalyticsEngine analyticsEngine;
    private EventMonitor eventMonitor;

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
    private Subscription flushSubscription;
    @Mock
    private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventMonitor = EventMonitor.on(eventBus);
        when(scheduler.schedule(any(Action1.class), anyLong(), any(TimeUnit.class))).thenReturn(flushSubscription);

        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, analyticsProperties, scheduler,
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo));
    }

    @Test
    public void shouldScheduleToFlushEventDataOnFirstTrackingEvent() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen");

        verify(scheduler).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void flushActionShouldCallFlushOnAllProviders() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen");

        ArgumentCaptor<Action1> flushAction = ArgumentCaptor.forClass(Action1.class);
        verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(null);
        verify(analyticsProviderOne).flush();
        verify(analyticsProviderTwo).flush();
    }

    @Test
    public void successfulFlushShouldResetSubscription() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen");

        ArgumentCaptor<Action1> flushAction = ArgumentCaptor.forClass(Action1.class);
        verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(null);
        verify(flushSubscription).unsubscribe();
    }

    @Test
    public void shouldRescheduleFlushForTrackingEventAfterPreviousFlushFinished() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen1");

        InOrder inOrder = inOrder(scheduler, scheduler);

        ArgumentCaptor<Action1> flushAction = ArgumentCaptor.forClass(Action1.class);
        inOrder.verify(scheduler).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(null); // finishes the first flush

        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen2");
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen3");
        inOrder.verify(scheduler).schedule(any(Action1.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldNotScheduleFlushIfFlushIsAlreadyScheduled() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen1");
        eventMonitor.publish(EventQueue.SCREEN_ENTERED, "screen2");

        verify(scheduler, times(1)).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromOpenSessionEvents() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(scheduler).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromCloseSessionEvents() {
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(scheduler).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromPlaybackEvents() {
        eventMonitor.publish(EventQueue.PLAYBACK_SESSION, PlaybackSessionEvent.forPlay(new Track(), 1L, mock(TrackSourceInfo.class)));
        verify(scheduler).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromUIEvents() {
        eventMonitor.publish(EventQueue.UI, UIEvent.fromComment("screen", 1L));

        verify(scheduler).schedule(any(Action1.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }
}
