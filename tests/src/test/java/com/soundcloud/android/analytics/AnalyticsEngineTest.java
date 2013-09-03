package com.soundcloud.android.analytics;


import static com.soundcloud.android.Expect.expect;
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

    @Test
    public void shouldCallOpenSessionOnAllProvidersIfAnalyticsEnabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.openSession();
        verify(analyticsProviderOne).openSession();
        verify(analyticsProviderTwo).openSession();
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPreferenceDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallOpenSessionOnAnyProvidersIfAnalyticsPropertyDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotOpenCloseSessionOnAnyProvidersIfAnalyticsDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.openSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldCallCloseSessionOnAllProvidersIfAnalyticsEnabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.closeSession();
        verify(analyticsProviderOne).closeSession();
        verify(analyticsProviderTwo).closeSession();
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPreferenceDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsPropertyDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldNotCallCloseSessionOnAnyProvidersIfAnalyticsDisabled(){
        when(analyticsProperties.isAnalyticsDisabled()).thenReturn(true);
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(false);
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        analyticsEngine.closeSession();
        verifyZeroInteractions(analyticsProviderOne);
        verifyZeroInteractions(analyticsProviderTwo);
    }

    @Test
    public void shouldRegisterItselfAsListenerForPreferenceChanges(){
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(analyticsEngine);
    }

    @Test
    public void shouldUpdateAnalyticsPreferenceWhenChangeOccurs(){
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeFalse();
        when(sharedPreferences.getBoolean(Settings.ANALYTICS, true)).thenReturn(true);
        analyticsEngine.onSharedPreferenceChanged(sharedPreferences,Settings.ANALYTICS);
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeTrue();
    }

    @Test
    public void shouldNotUpdateAnalyticsPreferenceIfKeyIsNotAnalytics(){
        analyticsEngine = new AnalyticsEngine(analyticsProperties, sharedPreferences, analyticsProviderOne, analyticsProviderTwo);
        expect(analyticsEngine.isAnalyticsPreferenceEnabled()).toBeFalse();
        SharedPreferences secondSharedPreferences = mock(SharedPreferences.class);
        analyticsEngine.onSharedPreferenceChanged(secondSharedPreferences, Settings.ACCOUNT_SYNC_SETTINGS);
        verifyZeroInteractions(secondSharedPreferences);
    }

}
