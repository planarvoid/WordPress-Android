package com.soundcloud.android.settings;

import com.soundcloud.android.configuration.features.FeatureOperations;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import javax.inject.Inject;
import java.util.Map;

public class ConfigurationFeaturesActivity extends ScSettingsActivity {
    @Inject FeatureOperations featureOperations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        for (Map.Entry<String, Boolean> feature : featureOperations.list().entrySet()) {
            addPreference(screen, feature.getKey(), feature.getValue());
        }
        setPreferenceScreen(screen);
    }

    private void addPreference(PreferenceScreen screen, String name, Boolean enabled) {
        CheckBoxPreference featurePref = new CheckBoxPreference(this);
        featurePref.setTitle(name);
        featurePref.setChecked(enabled);
        featurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                featureOperations.update(preference.getTitle().toString(), (boolean) o);
                return true;
            }
        });

        screen.addPreference(featurePref);
    }
}