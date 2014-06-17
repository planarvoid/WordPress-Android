package com.soundcloud.android.preferences;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;

@RunWith(SoundCloudTestRunner.class)
public class SettingsActivityTest {


    private SettingsActivity activity;
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
        activity = new SettingsActivity(applicationProperties, eventBus, developerPreferences);
    }

    @Test
    public void shouldRemoveDeveloperPreferencesIfNotADebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        activity.onCreate(null);
        PreferenceScreen preferenceScreen = activity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(DeveloperPreferences.PREF_KEY)).toBeNull();
    }

    @Test
    public void shouldAddDeveloperPreferencesIfADebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        activity.onCreate(null);
        PreferenceScreen preferenceScreen = activity.getPreferenceScreen();
        expect(preferenceScreen.findPreference(DeveloperPreferences.PREF_KEY)).not.toBeNull();
    }

    @Test
    public void shouldSetupDeveloperPreferencesIfDebugBuild(){
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        activity.onCreate(null);
        verify(developerPreferences).setup(activity);
    }

    @Test
    public void shouldGoToStreamOnNavigationUp() throws Exception {
        activity.onNavigateUp();
        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        expect(nextStartedActivity.getAction()).toEqual(Actions.STREAM);
        expect(nextStartedActivity.getFlags()).toEqual(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void shouldFinishOnNavigationUp() throws Exception {
        activity.onNavigateUp();
        expect(activity.isFinishing()).toBeTrue();
    }

}
