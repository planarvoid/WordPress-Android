package com.soundcloud.android.analytics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Scheduler;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class AnalyticsEngineEventFlushingTest extends AndroidUnitTest {
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SharedPreferences sharedPreferences;
    @Mock private AnalyticsProvider analyticsProviderOne;
    @Mock private AnalyticsProvider analyticsProviderTwo;
    @Mock private Scheduler scheduler;
    @Mock private Scheduler.Worker schedulerWorker;
    @Mock private AnalyticsProviderFactory analyticsProviderFactory;
    @Mock private Activity activity;
    @Captor private ArgumentCaptor<Action0> flushAction;

    @Before
    public void setUp() throws Exception {
        when(scheduler.createWorker()).thenReturn(schedulerWorker);
        when(schedulerWorker.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(new BooleanSubscription());

        when(analyticsProviderFactory.getProviders()).thenReturn(Arrays.asList(analyticsProviderOne, analyticsProviderTwo));
        new AnalyticsEngine(eventBus, sharedPreferences, scheduler, analyticsProviderFactory);
    }

    @Test
    public void shouldScheduleToFlushEventDataOnFirstTrackingEvent() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void flushActionShouldCallFlushOnAllProviders() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        verify(schedulerWorker).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call();
        verify(analyticsProviderOne).flush();
        verify(analyticsProviderTwo).flush();
    }

    @Test
    public void shouldRescheduleFlushForTrackingEventAfterPreviousFlushFinished() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        InOrder inOrder = inOrder(schedulerWorker);

        inOrder.verify(schedulerWorker).schedule(flushAction.capture(), anyLong(), any(TimeUnit.class));
        flushAction.getValue().call(); // finishes the first flush

        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        inOrder.verify(schedulerWorker).schedule(any(Action0.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldNotScheduleFlushIfFlushIsAlreadyScheduled() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        verify(schedulerWorker, times(1)).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromOpenSessionEvents() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromCloseSessionEvents() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromTrackingEvents() throws CreateModelException {
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldScheduleFlushesFromUIEvents() {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromComment("screen", 1L, null));

        verify(schedulerWorker).schedule(any(Action0.class), eq(AnalyticsEngine.FLUSH_DELAY_SECONDS), eq(TimeUnit.SECONDS));
    }
}
