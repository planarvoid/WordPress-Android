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
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.preferences.GeneralPreferences;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Scheduler;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTrackingTest {

    private AnalyticsEngine analyticsEngine;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SharedPreferences sharedPreferences;
    @Mock private AnalyticsProvider analyticsProviderOne;
    @Mock private AnalyticsProvider analyticsProviderTwo;
    @Mock private AnalyticsProvider analyticsProviderThree;
    @Mock private Scheduler scheduler;
    @Mock private Scheduler.Worker worker;
    @Mock private AnalyticsProviderFactory providersFactory;

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
        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(Activity.class);
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
    public void shouldTrackSearchEvent() {
        SearchEvent searchEvent = SearchEvent.searchSuggestion(Content.TRACK, true);
        eventBus.publish(EventQueue.SEARCH, searchEvent);

        verify(analyticsProviderOne, times(1)).handleSearchEvent(searchEvent);
        verify(analyticsProviderTwo, times(1)).handleSearchEvent(searchEvent);
    }

    @Test
    public void shouldUpdateProvidersOnSharedPreferenceChanged() {
        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());

        when(providersFactory.getProviders()).thenReturn(Arrays.asList(analyticsProviderThree));
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, GeneralPreferences.ANALYTICS_ENABLED);

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
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleSearchEvent(any(SearchEvent.class));

        eventBus.publish(EventQueue.TRACKING, TestEvents.unspecifiedTrackingEvent());
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
        eventBus.publish(EventQueue.SEARCH, SearchEvent.popularTagSearch("search"));

        verify(analyticsProviderTwo).handleTrackingEvent(any(TrackingEvent.class));
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        verify(analyticsProviderTwo).handleOnboardingEvent(any(OnboardingEvent.class));
        verify(analyticsProviderTwo).handleSearchEvent(any(SearchEvent.class));
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, scheduler, providersFactory);
    }

}
