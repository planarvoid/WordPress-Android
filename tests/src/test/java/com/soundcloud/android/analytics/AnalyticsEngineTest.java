package com.soundcloud.android.analytics;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.analytics.AnalyticsEngine.CloudPlayerStateWrapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsEngineTest {
    private AnalyticsEngine analyticsEngine;
    @Mock
    private AnalyticsProperties analyticsProperties;
    @Mock
    private AnalyticsProvider analyticsProviderOne;
    @Mock
    private AnalyticsProvider analyticsProviderTwo;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private CloudPlayerStateWrapper playbackWrapper;

    @Test
    public void shouldCallOpenSessionOnAllProvidersIfAnalyticsEnabled(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForActivity();
        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPreferenceDisabled(){
        setAnalyticsPropertyEnabledPreferenceDisabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    private void setAnalyticsPropertyEnabledPreferenceDisabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPropertyDisabled(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    private void setAnalyticsPropertyDisabledPreferenceEnabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
    }

    @Test
    public void shouldNotOpenCloseSessionOnAnyProvidersIfAnalyticsDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldCallCloseSessionOnAllProvidersIfAnalyticsEnabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verify(analyticsProviderOne).closeSession();
        verify(analyticsProviderTwo).closeSession();
    }

    @Test
    public void shouldNotCallCloseSessionOnAllProvidersIfAnalyticsEnabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPreferenceDisabledAndPlayerIsNotPlaying(){
        setAnalyticsPropertyEnabledPreferenceDisabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPreferenceDisabledAndPlayerIsPlaying(){
        setAnalyticsPropertyEnabledPreferenceDisabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPropertyDisabledAndPlayerIsNotPlaying(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPropertyDisabledAndPlayerIsPlaying(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldRegisterItselfAsListenerForPreferenceChanges(){
        initialiseAnalyticsEngine();
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(analyticsEngine);
    }

    @Test
    public void shouldUpdateAnalyticsPreferenceWhenChangeOccurs(){
        initialiseAnalyticsEngine();
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeFalse();
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences,Settings.ANALYTICS);
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateAnalyticsPreferenceIfKeyIsNotAnalytics(){
        initialiseAnalyticsEngine();
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeFalse();
        SharedPreferences secondSharedPreferences = mock(SharedPreferences.class);
        analyticsEngine.onSharedPreferenceChanged(secondSharedPreferences, Settings.ACCOUNT_SYNC_SETTINGS);
        verifyZeroInteractions(secondSharedPreferences);
    }

    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySession(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        analyticsEngine.openSessionForActivity();

        expect(analyticsEngine.activitySessionIsClosed()).toBeFalse();

    }

    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySessionAndAnalyticsDisabled(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        analyticsEngine.openSessionForActivity();

        expect(analyticsEngine.activitySessionIsClosed()).toBeFalse();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsEnabled(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForActivity();

        expect(analyticsEngine.activitySessionIsClosed()).toBeTrue();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySessionAndAnalyticsIsDisabled(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngine();
        analyticsEngine.closeSessionForActivity();

        expect(analyticsEngine.activitySessionIsClosed()).toBeTrue();
    }

    @Test
    public void shouldOpenAnalyticsSessionFromPlayerWhenAnalyticsEnabled(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForPlayer();
        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotOpenAnalyticsSessionFromPlayerWhenAnalyticsIsDisabled(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForPlayer();
        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldNotOpenAnalyticsSessionFromPlayerWhenAnalyticsIsEnabledAndPreferenceDisabled(){
        setAnalyticsPropertyEnabledPreferenceDisabled();
        initialiseAnalyticsEngine();
        analyticsEngine.openSessionForPlayer();
        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldCloseSessionIfAnalyticsEnabledAndActivitySessionStateIsFalse(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(false);

        analyticsEngine.closeSessionForPlayer();
        verify(analyticsProviderOne, times(2)).closeSession();
        verify(analyticsProviderTwo, times(2)).closeSession();
    }

    @Test
    public void shouldNotCloseSessionIfAnalyticsPropertyDisabledAndActivitySessionStateIsTrue(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();
        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldNotCloseSessionIfAnalyticsPreferenceDisabledAndActivitySessionStateIsTrue(){
        setAnalyticsPropertyDisabledPreferenceEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();
        verifyZeroInteractions(analyticsProviderOne, analyticsProviderTwo);
    }

    @Test
    public void shouldNotCloseSessionIfAnalyticsEnabledAndActivitySessionStateIsTrue(){
        setAnalyticsEnabled();
        initialiseAnalyticsEngineWithActivitySessionState(true);

        analyticsEngine.closeSessionForPlayer();
        verify(analyticsProviderOne, never()).closeSession();
        verify(analyticsProviderTwo, never()).closeSession();
    }

    private void setAnalyticsEnabled() {
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
    }

    private void initialiseAnalyticsEngineWithActivitySessionState(boolean state) {
        initialiseAnalyticsEngine();
        if (state){
            analyticsEngine.openSessionForActivity();
        } else {
            analyticsEngine.closeSessionForActivity();
        }
    }

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(analyticsProperties, playbackWrapper, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
    }

}
