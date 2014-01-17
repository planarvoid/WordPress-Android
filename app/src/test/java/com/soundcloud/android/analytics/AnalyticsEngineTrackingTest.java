package com.soundcloud.android.analytics;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Scheduler;

import android.app.Activity;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTrackingTest {
    @SuppressWarnings("unused") // it repsonds to tracking events via Rx, but must be alive to do so
    private AnalyticsEngine analyticsEngine;
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

    @After
    public void tearDown() {
        analyticsEngine.unsubscribeFromEvents();
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsDisabledForBuild() {
        setAnalyticsEnabledViaSettings();
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsEnabledForBuildButDisabledInSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldUnsubscribeFromEventsIfAnalyticsWasEnabledThenBecomesDisabledViaSettings() {
        setAnalyticsEnabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should arrive
        final ActivityLifeCycleEvent event1 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        EventBus.ACTIVITY_LIFECYCLE.publish(event1);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should NOT arrive
        final ActivityLifeCycleEvent event2 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        EventBus.ACTIVITY_LIFECYCLE.publish(event2);

        verify(analyticsProviderOne).handleActivityLifeCycleEvent(event1);
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(event1);
        verify(analyticsProviderOne, never()).handleActivityLifeCycleEvent(event2);
        verify(analyticsProviderTwo, never()).handleActivityLifeCycleEvent(event2);
    }

    @Test
    public void shouldResubscribeToEventsIfAnalyticsWasDisabledThenBecomesEnabledViaSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should not arrive
        final ActivityLifeCycleEvent event1 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        EventBus.ACTIVITY_LIFECYCLE.publish(event1);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should arrive
        final ActivityLifeCycleEvent event2 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        EventBus.ACTIVITY_LIFECYCLE.publish(event2);

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
    public void shouldNotSubscribeMultipleTimesToEventsWhenSharedPreferencesChangeAndAnalyticsAlreadyEnabled() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(analyticsProviderOne, times(1)).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
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
        EventBus.ACTIVITY_LIFECYCLE.publish(event);

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
        EventBus.PLAYBACK.publish(playbackEvent);

        verify(analyticsProviderOne, times(1)).handlePlaybackEvent(playbackEvent);
        verify(analyticsProviderTwo, times(1)).handlePlaybackEvent(playbackEvent);
    }

    @Test
    public void shouldTrackUIEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 0);
        EventBus.UI.publish(uiEvent);

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

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.PLAYBACK.publish(PlaybackEvent.forPlay(mock(Track.class), 0, mock(TrackSourceInfo.class)));
        EventBus.SCREEN_ENTERED.publish("screen");
        EventBus.UI.publish(UIEvent.fromToggleFollow(true, "screen", 0));
        EventBus.ONBOARDING.publish(OnboardingEvent.authComplete());

        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        verify(analyticsProviderTwo).handlePlaybackEvent(any(PlaybackEvent.class));
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
        analyticsEngine = new AnalyticsEngine(sharedPreferences, analyticsProperties, scheduler,
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo));
    }

}
