package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.properties.ApplicationProperties;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import javax.inject.Inject;

public class SettingsFragment extends PreferenceFragment {

    @Inject ApplicationProperties appProperties;

    @Inject GeneralSettings generalSettings;
    @Inject DeveloperSettings developerSettings;

    public static SettingsFragment create() {
        return new SettingsFragment();
    }

    public SettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        generalSettings.addTo(this);

        if (!appProperties.isReleaseBuild()) {
            addPreferencesFromResource(R.xml.settings_extras);
        }

        if (appProperties.isDebugBuild()) {
            developerSettings.addTo(this);
        }
    }

}
