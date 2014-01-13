package com.soundcloud.android.analytics;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Scheduler;

import android.app.Activity;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineSessionHandlingTest {
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
    public void tearDown(){
        AnalyticsEngine.ACTIVITY_SESSION_OPEN.set(false);
    }

    @Test
    public void shouldCallOpenSessionOnAllProvidersWhenActivityCreated() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldCallOpenSessionOnAllProvidersWhenActivityResumed() {
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnResume(Activity.class));

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsDisabled(){
        setAnalyticsDisabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnResume(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldCallCloseSessionOnAllProvidersWhenActivityPausedAndPlayerNotPlaying(){
        setAnalyticsEnabledViaSettings();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verify(analyticsProviderOne).closeSession();
        verify(analyticsProviderTwo).closeSession();
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfPlayerIsPlaying(){
        setAnalyticsEnabledViaSettings();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsNotPlaying(){
        setAnalyticsDisabledViaSettings();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsPlaying(){
        setAnalyticsDisabledViaSettings();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySession(){
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        expect(analyticsEngine.isActivitySessionClosed()).toBeFalse();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsEnabled(){
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        expect(analyticsEngine.isActivitySessionClosed()).toBeTrue();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsIsDisabled(){
        setAnalyticsDisabledViaSettings();
        initialiseAnalyticsEngine();

        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        expect(analyticsEngine.isActivitySessionClosed()).toBeTrue();
    }

    @Test
    public void shouldOpenPlayerSessionWhenAnalyticsEnabled(){
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngine();

        analyticsEngine.openSessionForPlayer();

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotOpenPlayerSessionWhenAnalyticsIsDisabled(){
        setAnalyticsDisabledViaSettings();
        initialiseAnalyticsEngine();

        analyticsEngine.openSessionForPlayer();

        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldClosePlayerSessionIfAnalyticsEnabledAndNoActivitySessionIsOpen(){
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        analyticsEngine.closeSessionForPlayer();

        verify(analyticsProviderOne, times(2)).closeSession();
        verify(analyticsProviderTwo, times(2)).closeSession();
    }

    @Test
    public void shouldNotClosePlayerSessionIfAnalyticsDisabledAndActivitySessionIsOpen(){
        setAnalyticsDisabledViaSettings();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();

        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldNotClosePlayerSessionIfAnalyticsEnabledAndActivitySessionIsOpen(){
        setAnalyticsEnabledViaSettings();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();

        verify(analyticsProviderOne, never()).closeSession();
        verify(analyticsProviderTwo, never()).closeSession();
    }

    private void setAnalyticsDisabledViaSettings() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(false);
    }

    private void setAnalyticsEnabledViaSettings() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(sharedPreferences.getBoolean(eq(SettingsActivity.ANALYTICS_ENABLED), anyBoolean())).thenReturn(true);
    }

    private void initialiseAnalyticsEngineWithActivitySessionState(boolean state) {
        initialiseAnalyticsEngine();
        if (state){
            EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        } else {
            EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));
        }
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(sharedPreferences, analyticsProperties, playbackWrapper, scheduler,
                analyticsProviderOne, analyticsProviderTwo);
    }

}
