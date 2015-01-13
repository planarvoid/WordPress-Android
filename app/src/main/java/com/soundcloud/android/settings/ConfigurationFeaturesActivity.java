package com.soundcloud.android.settings;

import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureOperations;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import javax.inject.Inject;

public class ConfigurationFeaturesActivity extends ScSettingsActivity {
    @Inject FeatureOperations featureOperations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        for (Feature feature : featureOperations.listFeatures()) {
            addPreference(screen, feature);
        }
        setPreferenceScreen(screen);
    }

    private void addPreference(PreferenceScreen screen, Feature feature) {
        CheckBoxPreference featurePref = new CheckBoxPreference(this);
        featurePref.setTitle(feature.name);
        featurePref.setChecked(feature.enabled);
        featurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                featureOperations.update(new Feature(preference.getTitle().toString(), (boolean) o));
                return true;
            }
        });

        screen.addPreference(featurePref);
    }
}