package com.soundcloud.android.preferences;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.content.Intent;
import android.preference.PreferenceScreen;

@RunWith(SoundCloudTestRunner.class)
public class SettingsActivityTest {

    @Mock private ApplicationProperties applicationProperties;
    @Mock private GeneralPreferences generalPreferences;
    @Mock private DeveloperPreferences developerPreferences;
    @Mock private FeatureFlags featureFlags;

    @InjectMocks private SettingsActivity activity;

    @Test
    public void setupGeneralPreferencesOnAnyBuild() {
        activity.onCreate(null);
        verify(generalPreferences).setup(activity);
    }

    @Test
    public void setsUpDeveloperPreferencesIfDebugBuild() {
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        activity.onCreate(null);
        verify(developerPreferences).setup(activity);
    }

    @Test
    public void doesNotSetUpDeveloperPreferencesIfNotDebugBuild() {
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        activity.onCreate(null);
        verify(developerPreferences, never()).setup(activity);
    }

    @Test
    public void goesToStreamOnNavigationUp() {
        activity.onNavigateUp();
        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        expect(nextStartedActivity.getAction()).toEqual(Actions.STREAM);
        expect(nextStartedActivity.getFlags()).toEqual(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void finishesOnNavigationUp() {
        activity.onNavigateUp();
        expect(activity.isFinishing()).toBeTrue();
    }

}
