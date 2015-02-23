package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineSettingsStorage;

import android.preference.CheckBoxPreference;
import android.preference.Preference;

import javax.inject.Inject;

class OfflineSettings {

    private static final String WIFI_ONLY = "offline.wifiOnlySync";

    @Inject OfflineSettingsStorage offlineSettings;

    public void setup(final SettingsActivity settings) {
        settings.addPreferencesFromResource(R.xml.settings_offline);

        final CheckBoxPreference featurePref = (CheckBoxPreference) settings.findPreference(WIFI_ONLY);
        featurePref.setChecked(offlineSettings.isWifiOnlyEnabled());
        featurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                offlineSettings.setWifiOnlyEnabled((boolean) o);
                return true;
            }
        });
    }
}
