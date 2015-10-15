package com.soundcloud.android.you;

import static com.soundcloud.android.settings.SettingKey.CLEAR_CACHE;

import com.soundcloud.android.R;
import com.soundcloud.android.settings.ClearCacheDialog;
import com.soundcloud.android.settings.SettingsFragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class BasicSettingsFragment extends PreferenceFragment {

    public static BasicSettingsFragment create() {
        return new BasicSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_basic);
        setupClearCachePreference();
    }

    private void setupClearCachePreference() {
        getPreferenceScreen().findPreference(CLEAR_CACHE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ClearCacheDialog.show(getFragmentManager());
                return true;
            }
        });
    }
}
