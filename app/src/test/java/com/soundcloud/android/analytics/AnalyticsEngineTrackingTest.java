package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTrackingTest {
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
    private EventBus2 eventBus;
    @Mock
    private Subscription eventSubscription;

    @Before
    public void setUp() throws Exception {
        eventMonitor = EventMonitor.on(eventBus).withSubscription(eventSubscription);
        when(scheduler.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(Subscriptions.empty());
    }

    @After
    public void tearDown() {
        analyticsEngine.unsubscribeFromEvents();
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsDisabledForBuild() {
        setAnalyticsEnabledViaSettings();
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        initialiseAnalyticsEngine();

        eventMonitor.verifyNotSubscribedTo(EventQueue.ACTIVITY_LIFE_CYCLE);
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsEnabledForBuildButDisabledInSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();

        eventMonitor.verifyNotSubscribedTo(EventQueue.ACTIVITY_LIFE_CYCLE);
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldUnsubscribeFromEventsIfAnalyticsWasEnabledThenBecomesDisabledViaSettings() {
        setAnalyticsEnabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should arrive
        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should NOT arrive
        eventMonitor.verifyUnsubscribed();

        verify(analyticsProviderOne).handleActivityLifeCycleEvent(event);
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(event);
    }

    @Test
    public void shouldResubscribeToEventsIfAnalyticsWasDisabledThenBecomesEnabledViaSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should not arrive
        final ActivityLifeCycleEvent event1 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventMonitor.verifyNotSubscribedTo(EventQueue.ACTIVITY_LIFE_CYCLE);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        eventMonitor.verifySubscribedTo(EventQueue.ACTIVITY_LIFE_CYCLE);

        // send the second event; should arrive
        final ActivityLifeCycleEvent event2 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event2);

        verify(analyticsProviderOne, never()).handleActivityLifeCycleEvent(event1);
        verify(analyticsProviderTwo, never()).handleActivityLifeCycleEvent(event1);
        verify(analyticsProviderOne).handleActivityLifeCycleEvent(event2);
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(event2);
    }

    @Test
    public void shouldSubscribeToSharedPreferenceChanges() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(analyticsEngine);
    }

    @Test
    public void shouldOnlyHandleAnalyticsEnabledSettingInSharedPreferenceChangedCallback() {
        initialiseAnalyticsEngine();

        SharedPreferences preferences = mock(SharedPreferences.class);
        analyticsEngine.onSharedPreferenceChanged(preferences, "wrong key");

        verify(preferences, never()).getBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldTrackCurrentUserChangedEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        final CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();
        EventBus.CURRENT_USER_CHANGED.publish(event);

        verify(analyticsProviderOne, times(1)).handleCurrentUserChangedEvent(event);
        verify(analyticsProviderTwo, times(1)).handleCurrentUserChangedEvent(event);
    }

    @Test
    public void shouldTrackActivityLifeCycleEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event);

        verify(analyticsProviderOne, times(1)).handleActivityLifeCycleEvent(event);
        verify(analyticsProviderTwo, times(1)).handleActivityLifeCycleEvent(event);
    }

    @Test
    public void shouldTrackScreenEvent() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, times(1)).handleScreenEvent(eq("screen"));
        verify(analyticsProviderTwo, times(1)).handleScreenEvent(eq("screen"));
    }

    @Test
    public void shouldTrackPlaybackEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        PlaybackEvent playbackEvent = PlaybackEvent.forPlay(mock(Track.class), 0, Mockito.mock(TrackSourceInfo.class));

        eventMonitor.publish(EventQueue.PLAYBACK, playbackEvent);

        verify(analyticsProviderOne, times(1)).handlePlaybackEvent(playbackEvent);
        verify(analyticsProviderTwo, times(1)).handlePlaybackEvent(playbackEvent);
    }

    @Test
    public void shouldTrackUIEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 0);
        eventMonitor.publish(EventQueue.UI, uiEvent);

        verify(analyticsProviderOne, times(1)).handleUIEvent(uiEvent);
        verify(analyticsProviderTwo, times(1)).handleUIEvent(uiEvent);
    }

    @Test
    public void shouldTrackOnboardingEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        OnboardingEvent onboardingEvent = OnboardingEvent.authComplete();
        EventBus.ONBOARDING.publish(onboardingEvent);

        verify(analyticsProviderOne, times(1)).handleOnboardingEvent(onboardingEvent);
        verify(analyticsProviderTwo, times(1)).handleOnboardingEvent(onboardingEvent);
    }

    @Test
    public void shouldIsolateProvidersExceptions() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        doThrow(new RuntimeException()).when(analyticsProviderOne).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handlePlaybackEvent(any(PlaybackEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleScreenEvent(anyString());
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleUIEvent(any(UIEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleOnboardingEvent(any(OnboardingEvent.class));

        eventMonitor.publish(EventQueue.PLAYBACK, PlaybackEvent.forPlay(mock(Track.class), 0, mock(TrackSourceInfo.class)));
        eventMonitor.publish(EventQueue.UI, UIEvent.fromToggleFollow(true, "screen", 0));

        eventMonitor.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.SCREEN_ENTERED.publish("screen");
        EventBus.ONBOARDING.publish(OnboardingEvent.authComplete());

        verify(analyticsProviderTwo).handlePlaybackEvent(any(PlaybackEvent.class));
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        verify(analyticsProviderTwo).handleScreenEvent(anyString());
        verify(analyticsProviderTwo).handleUIEvent(any(UIEvent.class));
        verify(analyticsProviderTwo).handleOnboardingEvent(any(OnboardingEvent.class));
    }

    private void setAnalyticsDisabledViaSettings() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
    }

    private void setAnalyticsEnabledViaSettings() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, analyticsProperties, scheduler,
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo));
    }

}
