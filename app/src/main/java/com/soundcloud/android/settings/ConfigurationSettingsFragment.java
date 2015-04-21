package com.soundcloud.android.settings;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import javax.inject.Inject;

public class ConfigurationSettingsFragment extends PreferenceFragment {

    @Inject FeatureOperations featureOperations;

    public static ConfigurationSettingsFragment create() {
        return new ConfigurationSettingsFragment();
    }

    public ConfigurationSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());

        for (String feature : featureOperations.listFeatures()) {
            boolean enabled = featureOperations.isFeatureEnabled(feature);
            addPreference(screen, feature, enabled);
        }
        setPreferenceScreen(screen);
    }

    private void addPreference(PreferenceScreen screen, String name, Boolean enabled) {
        CheckBoxPreference featurePref = new CheckBoxPreference(getActivity());
        featurePref.setTitle(name);
        featurePref.setChecked(enabled);
        featurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                featureOperations.updateFeature(preference.getTitle().toString(), (boolean) o);
                return true;
            }
        });
        screen.addPreference(featurePref);
    }

}
