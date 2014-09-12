package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.ads.AdCompanionImpressionController;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineEventFlushingTest {
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SharedPreferences sharedPreferences;
    @Mock private AnalyticsProvider analyticsProviderOne;
    @Mock private AnalyticsProvider analyticsProviderTwo;
    @Mock private Scheduler scheduler;
    @Mock private Scheduler.Worker schedulerWorker;
    @Mock private Subscription flushSubscription;
    @Mock private AdCompanionImpressionController adCompanionImpressionController;
    @Mock private AnalyticsProviderFactory analyticsProviderFactory;
    @Captor private ArgumentCaptor<Action0> flushAction;

    @Before
    public void setUp() throws Exception {
        when(adCompanionImpressionController.companionImpressionEvent()).thenReturn(Observable.<AudioAdCompanionImpressionEvent>empty());
        when(scheduler.createWorker()).thenReturn(schedulerWorker);
        when(schedulerWorker.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(flushSubscription);

        when(analyticsProviderFactory.getProviders()).thenReturn(Arrays.asList(analyticsProviderOne, analyticsProviderTwo));
        new AnalyticsEngine(eventBus, sharedPreferences, scheduler, adCompanionImpressionController, analyticsProviderFactory);
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
    public void shouldScheduleFlushesFromPlaybackEvents() throws CreateModelException {
        PlaybackSessionEvent playEvent = TestEvents.playbackSessionPlayEvent();
        eventBus.publish(EventQueue.PLAYBACK_SESSION, playEvent);
        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromUIEvents() {
        eventBus.publish(EventQueue.UI, UIEvent.fromComment("screen", 1L));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }
}
