package com.soundcloud.android.analytics;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Scheduler;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineSessionHandlingTest {
    private AnalyticsEngine analyticsEngine;
    @Mock
    private AnalyticsProperties analyticsProperties;
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
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldCallOpenSessionOnAllProvidersWhenActivityResumed() {
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnResume(Activity.class));

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsDisabled(){
        setAnalyticsDisabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnResume(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldCallCloseSessionOnAllProvidersWhenActivityPausedAndPlayerNotPlaying(){
        setAnalyticsEnabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verify(analyticsProviderOne).closeSession();
        verify(analyticsProviderTwo).closeSession();
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfPlayerIsPlaying(){
        setAnalyticsEnabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsNotPlaying(){
        setAnalyticsDisabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsPlaying(){
        setAnalyticsDisabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySession(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        expect(analyticsEngine.activitySessionIsClosed()).toBeFalse();
    }

    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySessionAndAnalyticsDisabled(){
        setAnalyticsDisabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        expect(analyticsEngine.activitySessionIsClosed()).toBeFalse();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsEnabled(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        expect(analyticsEngine.activitySessionIsClosed()).toBeTrue();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsIsDisabled(){
        setAnalyticsDisabled();
        initialiseAnalyticsEngine();

        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));

        expect(analyticsEngine.activitySessionIsClosed()).toBeTrue();
    }

    @Test
    public void shouldOpenPlayerSessionWhenAnalyticsEnabled(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();

        analyticsEngine.openSessionForPlayer();

        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotOpenPlayerSessionWhenAnalyticsIsDisabled(){
        setAnalyticsDisabled();
        initialiseAnalyticsEngine();

        analyticsEngine.openSessionForPlayer();

        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldClosePlayerSessionIfAnalyticsEnabledAndNoActivitySessionIsOpen(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        analyticsEngine.closeSessionForPlayer();

        verify(analyticsProviderOne, times(2)).closeSession();
        verify(analyticsProviderTwo, times(2)).closeSession();
    }

    @Test
    public void shouldNotClosePlayerSessionIfAnalyticsDisabledAndActivitySessionIsOpen(){
        setAnalyticsDisabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();

        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldNotClosePlayerSessionIfAnalyticsEnabledAndActivitySessionIsOpen(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();

        verify(analyticsProviderOne, never()).closeSession();
        verify(analyticsProviderTwo, never()).closeSession();
    }

    private void setAnalyticsDisabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(analyticsProperties.isAnalyticsEnabled()).thenReturn(false);
    }

    private void setAnalyticsEnabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(analyticsProperties.isAnalyticsEnabled()).thenReturn(true);
    }

    private void initialiseAnalyticsEngineWithActivitySessionState(boolean state) {
        initialiseAnalyticsEngine();
        if (state){
            Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        } else {
            Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(Activity.class));
        }
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(analyticsProperties, playbackWrapper, scheduler,
                analyticsProviderOne, analyticsProviderTwo);
    }

}
