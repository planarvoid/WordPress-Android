package com.soundcloud.android.settings;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SettingsFragmentTest {

    @Mock private ApplicationProperties applicationProperties;
    @Mock private GeneralSettings generalSettings;
    @Mock private DeveloperSettings developerSettings;

    @InjectMocks SettingsFragment fragment;

    @Test
    public void setupGeneralPreferencesOnAnyBuild() {
        fragment.onCreate(null);
        verify(generalSettings).addTo(fragment);
    }

    @Test
    public void setsUpDeveloperPreferencesIfDebugBuild() {
        when(applicationProperties.isDebugBuild()).thenReturn(true);
        fragment.onCreate(null);
        verify(developerSettings).addTo(fragment);
    }

    @Test
    public void doesNotSetUpDeveloperPreferencesIfNotDebugBuild() {
        when(applicationProperties.isDebugBuild()).thenReturn(false);
        fragment.onCreate(null);
        verify(developerSettings, never()).addTo(fragment);
    }

}