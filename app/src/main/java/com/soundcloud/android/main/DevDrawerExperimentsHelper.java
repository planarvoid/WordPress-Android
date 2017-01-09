package com.soundcloud.android.main;

import static com.soundcloud.java.strings.Strings.isNullOrEmpty;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ActiveExperiments;
import com.soundcloud.android.configuration.experiments.ExperimentConfiguration;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import javax.inject.Inject;
import java.util.List;

class DevDrawerExperimentsHelper {

    private final Resources resources;
    private final ExperimentOperations experimentOperations;

    @Inject
    DevDrawerExperimentsHelper(Resources resources, ExperimentOperations experimentOperations) {
        this.resources = resources;
        this.experimentOperations = experimentOperations;
    }

    void addExperiments(PreferenceScreen screen) {
        PreferenceCategory category = new PreferenceCategory(screen.getContext());
        category.setTitle(resources.getString(R.string.dev_drawer_section_experiments));
        category.setKey(getExperimentsKey());
        screen.addPreference(category);

        for (final ExperimentConfiguration experiment : ActiveExperiments.ACTIVE_EXPERIMENTS) {
            ListPreference listPreference = buildExperimentListPreference(screen, experiment);
            category.addPreference(listPreference);
        }
    }

    private ListPreference buildExperimentListPreference(final PreferenceScreen screen,
                                                         final ExperimentConfiguration experiment) {
        ListPreference listPreference = new ListPreference(screen.getContext());
        String experimentKey = getExperimentLayerPrefix(experiment.getLayerName()) + experiment.getName();

        listPreference.setKey(experimentKey);
        listPreference.setTitle(ScTextUtils.fromSnakeCaseToCamelCase(experiment.getName()));
        setExperimentEntries(listPreference, experiment);
        setExperimentSummary(listPreference, experiment);

        listPreference.setOnPreferenceChangeListener((preference, variation) -> {
            clearPreferencesForExperimentLayer(screen, experiment.getLayerName());
            experimentOperations.forceExperimentVariation(experiment, (String) variation);
            setExperimentSummary((ListPreference) preference, experiment);
            return true;
        });

        return listPreference;
    }

    private void setExperimentEntries(ListPreference listPreference, ExperimentConfiguration experiment) {
        List<String> variations = experiment.getVariations();
        int entriesCount = variations.size();
        CharSequence[] entryLabels = variations.toArray(new CharSequence[entriesCount + 1]);
        CharSequence[] entryValues = variations.toArray(new CharSequence[entriesCount + 1]);

        entryLabels[entriesCount] = resources.getString(R.string.dev_drawer_section_experiment_default);
        entryValues[entriesCount] = Strings.EMPTY;

        listPreference.setEntries(entryLabels);
        listPreference.setEntryValues(entryValues);
    }

    private void clearPreferencesForExperimentLayer(PreferenceScreen screen, String layer) {
        PreferenceCategory experimentPreferences = (PreferenceCategory) screen.findPreference(getExperimentsKey());
        String layerPrefix = getExperimentLayerPrefix(layer);

        for (int i = 0; i < experimentPreferences.getPreferenceCount(); i++) {
            ListPreference preference = (ListPreference) experimentPreferences.getPreference(i);
            if (preference.getKey().startsWith(layerPrefix)) {
                clearExperimentSummary(preference);
            }
        }
    }

    private String getExperimentLayerPrefix(String layer) {
        return resources.getString(R.string.dev_drawer_section_experiments_layer_prefix_key, layer);
    }

    private String getExperimentsKey() {
        return resources.getString(R.string.dev_drawer_section_experiments_key);
    }

    private void setExperimentSummary(ListPreference preference, ExperimentConfiguration experiment) {
        String currentVariant = experimentOperations.getExperimentVariant(experiment);

        if (!isNullOrEmpty(currentVariant)) {
            preference.setSummary(resources.getString(R.string.dev_drawer_section_experiments_enabled_prefix,
                                                      currentVariant));
            preference.setValue(currentVariant);
        } else {
            clearExperimentSummary(preference);
        }
    }

    private void clearExperimentSummary(ListPreference preference) {
        preference.setSummary(resources.getString(R.string.dev_drawer_section_experiment_default));
        preference.setValue(Strings.EMPTY);
    }

}
