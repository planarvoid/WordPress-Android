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
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;
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
    private AnalyticsEngine.PlaybackServiceStateWrapper playbackWrapper;
    @Mock
    private Scheduler scheduler;

    @After
    public void tearDown() {
        AnalyticsEngine.ACTIVITY_SESSION_OPEN.set(false);
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
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should NOT arrive
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(analyticsProviderOne, times(1)).openSession();
        verify(analyticsProviderTwo, times(1)).openSession();
    }

    @Test
    public void shouldResubscribeToEventsIfAnalyticsWasDisabledThenBecomesEnabledViaSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should not arrive
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should arrive
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(analyticsProviderOne, times(1)).openSession();
        verify(analyticsProviderTwo, times(1)).openSession();
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

        verify(analyticsProviderOne, times(1)).openSession();
    }

    @Test
    public void shouldOnlyHandleAnalyticsEnabledSettingInSharedPreferenceChangedCallback() {
        initialiseAnalyticsEngine();

        SharedPreferences preferences = mock(SharedPreferences.class);
        analyticsEngine.onSharedPreferenceChanged(preferences, "wrong key");

        verify(preferences, never()).getBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldTrackScreenForAllProvidersWhenScreenEnteredAndSessionIsOpen() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne).trackScreen(eq("screen"));
        verify(analyticsProviderTwo).trackScreen(eq("screen"));
    }

    @Test
    public void shouldNotTrackScreenIfNoSessionIsOpen() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.SCREEN_ENTERED.publish("screen");

        verify(analyticsProviderOne, never()).trackScreen(anyString());
        verify(analyticsProviderTwo, never()).trackScreen(anyString());
    }

    @Test
    public void shouldOnlySubscribeToEnterScreenEventOncePerSession() {
        setAnalyticsEnabledViaSettings();
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
        setAnalyticsEnabledViaSettings();
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
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        PlaybackEvent playbackEvent = PlaybackEvent.forPlay(mock(Track.class), 0, Mockito.mock(TrackSourceInfo.class));
        EventBus.PLAYBACK.publish(playbackEvent);

        verify(analyticsProviderOne, times(1)).trackPlaybackEvent(playbackEvent);
        verify(analyticsProviderTwo, times(1)).trackPlaybackEvent(playbackEvent);
    }

    @Test
    public void shouldTrackSocialEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        SocialEvent socialEvent = SocialEvent.fromFollow("screen", 0);
        EventBus.SOCIAL.publish(socialEvent);

        verify(analyticsProviderOne, times(1)).trackSocialEvent(socialEvent);
        verify(analyticsProviderTwo, times(1)).trackSocialEvent(socialEvent);
    }

    @Test
    public void shouldIsolateProvidersExceptions() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        doThrow(new RuntimeException()).when(analyticsProviderOne).openSession();
        doThrow(new RuntimeException()).when(analyticsProviderOne).trackPlaybackEvent(any(PlaybackEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).trackScreen(anyString());
        doThrow(new RuntimeException()).when(analyticsProviderOne).trackSocialEvent(any(SocialEvent.class));

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.PLAYBACK.publish(PlaybackEvent.forPlay(mock(Track.class), 0, mock(TrackSourceInfo.class)));
        EventBus.SCREEN_ENTERED.publish("screen");
        EventBus.SOCIAL.publish(SocialEvent.fromFollow("screen", 0));

        verify(analyticsProviderTwo).openSession();
        verify(analyticsProviderTwo).trackPlaybackEvent(any(PlaybackEvent.class));
        verify(analyticsProviderTwo).trackScreen(anyString());
        verify(analyticsProviderTwo).trackSocialEvent(any(SocialEvent.class));
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
        analyticsEngine = new AnalyticsEngine(sharedPreferences, analyticsProperties, playbackWrapper, scheduler,
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo));
    }

}
