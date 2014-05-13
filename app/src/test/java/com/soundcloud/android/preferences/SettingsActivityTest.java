package com.soundcloud.android.preferences;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.preference.Preference;
import android.preference.PreferenceScreen;

@RunWith(SoundCloudTestRunner.class)
public class SettingsActivityTest {


    private SettingsActivity settingsActivity;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private EventBus eventBus;
    @Mock
    private Preference preference;
    @Mock
    private DeveloperPreferences developerPreferences;

    @Before
    public void setUp() {
        settingsActivity = new SettingsActivity(applicationProperties, eventBus, developerPreferences);
    }

    @Test
    public void shouldRemoveDeveloperPreferencesIfNotADebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        settingsActivity.onCreate(null);
        PreferenceScreen preferenceScreen = settingsActivity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(DeveloperPreferences.PREF_KEY)).toBeNull();
    }

    @Test
    public void shouldAddDeveloperPreferencesIfADebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        settingsActivity.onCreate(null);
        PreferenceScreen preferenceScreen = settingsActivity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(DeveloperPreferences.PREF_KEY)).not.toBeNull();
    }

    @Test
    public void shouldRemoveExtrasPreferenceIfBuildIsRelease() {
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        when(applicationProperties.isReleaseBuild()).thenReturn(true);
        settingsActivity.onCreate(null);
        PreferenceScreen preferenceScreen = settingsActivity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(SettingsActivity.EXTRAS)).toBeNull();
    }


    @Test
    public void shouldNotRemoveExtrasPreferenceIfBuildIsNotRelease() {
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        when(applicationProperties.isReleaseBuild()).thenReturn(false);
        settingsActivity.onCreate(null);
        PreferenceScreen preferenceScreen = settingsActivity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(SettingsActivity.EXTRAS)).not.toBeNull();
    }

    @Test
    public void shouldSetupDeveloperPreferencesIfDebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        settingsActivity.onCreate(null);
        verify(developerPreferences).setup(settingsActivity);
    }
}
