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
import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.ads.AdCompanionImpressionController;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTrackingTest {

    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(123L));

    private AnalyticsEngine analyticsEngine;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private AnalyticsProperties analyticsProperties;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private AnalyticsProvider analyticsProviderOne;
    @Mock private AnalyticsProvider analyticsProviderTwo;
    @Mock private Scheduler scheduler;
    @Mock private Scheduler.Worker worker;
    @Mock private AdCompanionImpressionController adCompanionImpressionController;

    @Before
    public void setUp() throws Exception {
        when(adCompanionImpressionController.companionImpressionEvent()).thenReturn(Observable.<AudioAdCompanionImpressionEvent>empty());
        when(scheduler.createWorker()).thenReturn(worker);
        when(worker.schedule(any(Action0.class), anyLong(), any(TimeUnit.class))).thenReturn(Subscriptions.empty());
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsDisabledForBuild() {
        setAnalyticsEnabledViaSettings();
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        initialiseAnalyticsEngine();

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotSubscribeToEventsIfAnalyticsEnabledForBuildButDisabledInSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldUnsubscribeFromEventsIfAnalyticsWasEnabledThenBecomesDisabledViaSettings() {
        setAnalyticsEnabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should arrive
        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should NOT arrive
        eventBus.verifyUnsubscribed();

        verify(analyticsProviderOne).handleActivityLifeCycleEvent(event);
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(event);
    }

    @Test
    public void shouldResubscribeToEventsIfAnalyticsWasDisabledThenBecomesEnabledViaSettings() {
        setAnalyticsDisabledViaSettings();

        initialiseAnalyticsEngine();
        // send the first event; should not arrive
        final ActivityLifeCycleEvent event1 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event1);

        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        // send the second event; should arrive
        final ActivityLifeCycleEvent event2 = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event2);

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
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event);

        verify(analyticsProviderOne, times(1)).handleCurrentUserChangedEvent(event);
        verify(analyticsProviderTwo, times(1)).handleCurrentUserChangedEvent(event);
    }

    @Test
    public void shouldTrackActivityLifeCycleEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        final ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event);

        verify(analyticsProviderOne, times(1)).handleActivityLifeCycleEvent(event);
        verify(analyticsProviderTwo, times(1)).handleActivityLifeCycleEvent(event);
    }

    @Test
    public void shouldTrackScreenEvent() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen");

        verify(analyticsProviderOne, times(1)).handleScreenEvent(eq("screen"));
        verify(analyticsProviderTwo, times(1)).handleScreenEvent(eq("screen"));
    }

    @Test
    public void shouldTrackPlaybackEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        PlaybackSessionEvent playbackSessionEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, Urn.forUser(1L),
                Mockito.mock(TrackSourceInfo.class), 0, 0);

        eventBus.publish(EventQueue.PLAYBACK_SESSION, playbackSessionEvent);

        verify(analyticsProviderOne, times(1)).handlePlaybackSessionEvent(playbackSessionEvent);
        verify(analyticsProviderTwo, times(1)).handlePlaybackSessionEvent(playbackSessionEvent);
    }

    @Test
    public void shouldTrackUIEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 0);
        eventBus.publish(EventQueue.UI, uiEvent);

        verify(analyticsProviderOne, times(1)).handleUIEvent(uiEvent);
        verify(analyticsProviderTwo, times(1)).handleUIEvent(uiEvent);
    }

    @Test
    public void shouldTrackOnboardingEvent() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        OnboardingEvent onboardingEvent = OnboardingEvent.authComplete();
        eventBus.publish(EventQueue.ONBOARDING, onboardingEvent);

        verify(analyticsProviderOne, times(1)).handleOnboardingEvent(onboardingEvent);
        verify(analyticsProviderTwo, times(1)).handleOnboardingEvent(onboardingEvent);
    }

    @Test
    public void shouldTrackSearchEvent() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        SearchEvent searchEvent = SearchEvent.searchSuggestion(Content.TRACK, true);
        eventBus.publish(EventQueue.SEARCH, searchEvent);

        verify(analyticsProviderOne, times(1)).handleSearchEvent(searchEvent);
        verify(analyticsProviderTwo, times(1)).handleSearchEvent(searchEvent);
    }

    @Test
    public void shouldTrackPlayControlEvent() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        PlayControlEvent playControlEvent = PlayControlEvent.playerClickPlay();
        eventBus.publish(EventQueue.PLAY_CONTROL, playControlEvent);

        verify(analyticsProviderOne, times(1)).handlePlayControlEvent(playControlEvent);
        verify(analyticsProviderTwo, times(1)).handlePlayControlEvent(playControlEvent);
    }

    @Test
    public void shouldIsolateProvidersExceptions() throws Exception {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        doThrow(new RuntimeException()).when(analyticsProviderOne).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handlePlaybackSessionEvent(any(PlaybackSessionEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleScreenEvent(anyString());
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleUIEvent(any(UIEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleOnboardingEvent(any(OnboardingEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handleSearchEvent(any(SearchEvent.class));
        doThrow(new RuntimeException()).when(analyticsProviderOne).handlePlayControlEvent(any(PlayControlEvent.class));

        eventBus.publish(EventQueue.PLAYBACK_SESSION,
                PlaybackSessionEvent.forPlay(TRACK_DATA, Urn.forUser(1L), mock(TrackSourceInfo.class), 0, 0));
        eventBus.publish(EventQueue.UI, UIEvent.fromToggleFollow(true, "screen", 0));
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(Activity.class));
        eventBus.publish(EventQueue.SCREEN_ENTERED, "screen");
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
        eventBus.publish(EventQueue.SEARCH, SearchEvent.popularTagSearch("search"));
        eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.playerClickPlay());

        verify(analyticsProviderTwo).handlePlaybackSessionEvent(any(PlaybackSessionEvent.class));
        verify(analyticsProviderTwo).handleActivityLifeCycleEvent(any(ActivityLifeCycleEvent.class));
        verify(analyticsProviderTwo).handleScreenEvent(anyString());
        verify(analyticsProviderTwo).handleUIEvent(any(UIEvent.class));
        verify(analyticsProviderTwo).handleOnboardingEvent(any(OnboardingEvent.class));
        verify(analyticsProviderTwo).handleSearchEvent(any(SearchEvent.class));
        verify(analyticsProviderTwo).handlePlayControlEvent(any(PlayControlEvent.class));
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
                Lists.newArrayList(analyticsProviderOne, analyticsProviderTwo), adCompanionImpressionController);
    }

}
