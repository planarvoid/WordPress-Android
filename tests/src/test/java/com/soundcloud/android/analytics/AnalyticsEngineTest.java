package com.soundcloud.android.analytics;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.analytics.AnalyticsEngine.CloudPlayerStateWrapper;
import static org.mockito.Mockito.mock;
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
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.openSession();
        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPreferenceDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPropertyDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotOpenCloseSessionOnAnyProvidersIfAnalyticsDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldCallCloseSessionOnAllProvidersIfAnalyticsEnabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verify(analyticsProviderOne).closeSession();
        verify(analyticsProviderTwo).closeSession();
    }

    @Test
    public void shouldNotCallCloseSessionOnAllProvidersIfAnalyticsEnabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPreferenceDisabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPreferenceDisabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPropertyDisabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPropertyDisabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsNotPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(false);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabledAndPlayerIsPlaying(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        when(playbackWrapper.isPlayerPlaying()).thenReturn(true);
        initialiseAnalyticsEngine();
        analyticsEngine.closeSession();
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

    private void initialiseAnalyticsEngine() {
        analyticsEngine = new AnalyticsEngine(analyticsProperties, playbackWrapper, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
    }

}
