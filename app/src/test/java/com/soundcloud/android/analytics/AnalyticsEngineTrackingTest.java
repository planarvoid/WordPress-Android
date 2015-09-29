package com.soundcloud.android.analytics;


import static org.assertj.core.api.Assertions.assertThat;
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
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Scheduler;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Arrays;
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
    @Captor private ArgumentCaptor<UserSessionEvent> sessionEventCaptor;

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

        when(providersFactory.getProviders()).thenReturn(Arrays.asList(analyticsProviderThree));
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

    @Test
    public void userOpenSessionWhenApplicationStarts() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));

        verify(analyticsProviderOne).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void userOpenSessionWhenApplicationInForeground() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity));

        verify(analyticsProviderOne).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void userOpenSessionWhenApplicationInForegroundAndPlaySessionStopped() throws CreateModelException {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionStopEvent());

        verify(analyticsProviderOne).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void userOpenSessionWhenApplicationInBackgroundAndPlaySessionOpened() throws CreateModelException {
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionPlayEvent());
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity));

        verify(analyticsProviderOne).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void userOpenSessionWhenApplicationInBackgroundAndPlaySessionsStoppedForBuffering() throws CreateModelException {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_BUFFERING));

        verify(analyticsProviderOne, times(2)).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void doesNotSendDuplicateUserSessionEvent() throws CreateModelException {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_BUFFERING));
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_BUFFERING));

        verify(analyticsProviderOne, times(2)).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getAllValues().get(0)).isEqualTo(UserSessionEvent.CLOSED);
        assertThat(sessionEventCaptor.getAllValues().get(1)).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void sendOpenSessionWhenSessionIsReopened() throws CreateModelException {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity));
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity));
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity));

        verify(analyticsProviderOne, times(3)).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getAllValues().get(0)).isEqualTo(UserSessionEvent.OPENED);
        assertThat(sessionEventCaptor.getAllValues().get(1)).isEqualTo(UserSessionEvent.CLOSED);
        assertThat(sessionEventCaptor.getAllValues().get(2)).isEqualTo(UserSessionEvent.OPENED);
    }

    @Test
    public void userCloseSessionWhenApplicationInBackgroundAndPlaySessionsStopped() throws CreateModelException {
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionPlayEvent());
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity));
        eventBus.publish(EventQueue.TRACKING, TestEvents.playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_PAUSE));

        verify(analyticsProviderOne, times(2)).handleUserSessionEvent(sessionEventCaptor.capture());

        assertThat(sessionEventCaptor.getValue()).isEqualTo(UserSessionEvent.CLOSED);
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, scheduler, providersFactory);
    }

}
