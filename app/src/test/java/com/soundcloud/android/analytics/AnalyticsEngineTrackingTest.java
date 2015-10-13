package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Scheduler;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AnalyticsEngineTrackingTest extends AndroidUnitTest {

    private AnalyticsEngine analyticsEngine;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SharedPreferences sharedPreferences;
    @Mock private AnalyticsProvider analyticsProviderOne;
    @Mock private AnalyticsProvider analyticsProviderTwo;
    @Mock private AnalyticsProvider analyticsProviderThree;
    @Mock private Scheduler scheduler;
    @Mock private Scheduler.Worker worker;
    @Mock private AnalyticsProviderFactory providersFactory;
    @Mock private Activity activity;

    @Before
    public void setUp() throws Exception {
        when(scheduler.createWorker()).thenReturn(worker);
        when(worker.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(Subscriptions.empty());
        when(providersFactory.getProviders()).thenReturn(Arrays.asList(analyticsProviderOne, analyticsProviderTwo));

        initialiseAnalyticsEngine();
    }

    @Test
    public void shouldTrackCurrentUserChangedEvent() {
        final CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event);

        verify(analyticsProviderOne, times(1)).handleCurrentUserChangedEvent(event);
        verify(analyticsProviderTwo, times(1)).handleCurrentUserChangedEvent(event);
    }

    @Test
    public void shouldTrackActivityLifeCycleEvent() {
        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(activity);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event);

        verify(analyticsProviderOne, times(1)).handleActivityLifeCycleEvent(event);
        verify(analyticsProviderTwo, times(1)).handleActivityLifeCycleEvent(event);
    }

    @Test
    public void shouldTrackTrackingEvent() {
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        verify(analyticsProviderOne, times(1)).handleTrackingEvent(isA(TrackingEvent.class));
        verify(analyticsProviderTwo, times(1)).handleTrackingEvent(isA(TrackingEvent.class));
    }

    @Test
    public void shouldTrackOnboardingEvent() {
        OnboardingEvent onboardingEvent = OnboardingEvent.authComplete();
        eventBus.publish(EventQueue.ONBOARDING, onboardingEvent);

        verify(analyticsProviderOne, times(1)).handleOnboardingEvent(onboardingEvent);
        verify(analyticsProviderTwo, times(1)).handleOnboardingEvent(onboardingEvent);
    }

    @Test
    public void shouldUpdateProvidersOnSharedPreferenceChanged() {
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        when(providersFactory.getProviders()).thenReturn(Collections.singletonList(analyticsProviderThree));
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingKey.ANALYTICS_ENABLED);

        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        verify(analyticsProviderOne, times(1)).handleTrackingEvent(isA(TrackingEvent.class));
        verify(analyticsProviderTwo, times(1)).handleTrackingEvent(isA(TrackingEvent.class));
        verify(analyticsProviderThree, times(1)).handleTrackingEvent(isA(TrackingEvent.class));
    }

    @Test
    public void shouldSubscribeToSharedPreferenceChanges() {
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(analyticsEngine);
    }

    @Test
    public void shouldOnlyHandleAnalyticsEnabledSettingInSharedPreferenceChangedCallback() {
        SharedPreferences preferences = mock(SharedPreferences.class);
        analyticsEngine.onSharedPreferenceChanged(preferences, "wrong key");

        verify(providersFactory, times(1)).getProviders(); // Only setup providers in constructor
    }

    @Test
    public void shouldIsolateProvidersExceptions() throws CreateModelException {
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleTrackingEvent(any(TrackingEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleOnboardingEvent(any(OnboardingEvent.class));

        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());

        verify(analyticsProviderTwo).handleTrackingEvent(any(TrackingEvent.class));
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        verify(analyticsProviderTwo).handleOnboardingEvent(any(OnboardingEvent.class));
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, scheduler, providersFactory);
    }

}
