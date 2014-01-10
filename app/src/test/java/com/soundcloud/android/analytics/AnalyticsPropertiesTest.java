package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;
import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsPropertiesTest {

    private AnalyticsProperties analyticsProperties;
    @Mock
    private Resources resources;
    @Mock
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        when(resources.getString(R.string.localytics_app_key)).thenReturn("localyticsKey");
    }

    @Test
    public void shouldRetrieveLocalyticsKeyIfAnalyticsIsEnabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
        expect(analyticsProperties.getLocalyticsAppKey()).toBe("localyticsKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAnalyticsEnabledAndLocalyticsKeyIsEmpty(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        when(resources.getString(R.string.localytics_app_key)).thenReturn("");
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
    }

    @Test
    public void shouldSpecifyAnalyticsDisabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        expect(new AnalyticsProperties(resources, sharedPreferences).isAnalyticsDisabled()).toBeTrue();
    }

    @Test
    public void shouldRegisterItselfAsListenerForPreferenceChanges(){
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(analyticsProperties);
    }

    @Test
    public void shouldDisableAnalyticsIfEnabledViaBuildAndDisabledThroughSharedPreferences() {
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);

        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(false);
        analyticsProperties.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        expect(analyticsProperties.isAnalyticsEnabled()).toBeFalse();
    }

    @Test
    public void shouldEnableAnalyticsIfEnabledViaBuildAndEnabledThroughSharedPreferences() {
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);

        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(true);
        analyticsProperties.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        expect(analyticsProperties.isAnalyticsEnabled()).toBeTrue();
    }

    @Test
    public void shouldNotEnableAnalyticsIfDisabledViaBuildAndEnabledThroughSharedPreferences() {
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);

        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(true);
        analyticsProperties.onSharedPreferenceChanged(sharedPreferences, SettingsActivity.ANALYTICS_ENABLED);

        expect(analyticsProperties.isAnalyticsEnabled()).toBeFalse();
    }

    @Test
    public void shouldNotUpdateAnalyticsEnabledStateIfKeyIsNotAnalytics(){
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
        SharedPreferences changedSharedPreferences = mock(SharedPreferences.class);

        analyticsProperties.onSharedPreferenceChanged(changedSharedPreferences, SettingsActivity.ACCOUNT_SYNC_SETTINGS);

        verifyZeroInteractions(changedSharedPreferences);
        verify(sharedPreferences, never()).getBoolean(anyString(), anyBoolean());
    }
}
