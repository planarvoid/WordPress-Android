package com.soundcloud.android.main;

import static com.soundcloud.java.strings.Strings.isNullOrEmpty;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.experiments.ActiveExperiments;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import javax.inject.Inject;
import java.util.List;

class DevDrawerExperimentsHelper {
    private static final String VARIANT_DELIMITER = " : ";

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
        String experimentKey = getExperimentLayerPrefix(experiment.getLayerName()) + experiment.getExperimentName();

        listPreference.setKey(experimentKey);
        listPreference.setTitle(ScTextUtils.fromSnakeCaseToCamelCase(experiment.getExperimentName()));
        setExperimentEntries(listPreference, experiment);
        setExperimentSummary(listPreference, experiment);

        listPreference.setOnPreferenceChangeListener((preference, variant) -> {
            clearPreferencesForExperimentLayer(screen, experiment.getLayerName());
            experimentOperations.forceExperimentVariation(buildExperimentLayer(experiment, (String) variant));
            setExperimentSummary((ListPreference) preference, experiment);
            return true;
        });

        return listPreference;
    }

    private Layer buildExperimentLayer(ExperimentConfiguration experiment, String variant) {
        final String[] variantParts = variant.split(VARIANT_DELIMITER);
        final String variantName = variantParts[0];
        final int variantId = Integer.parseInt(variantParts[1]);

        return new Layer(
                experiment.getLayerName(),
                experiment.getExperimentId().or(Consts.NOT_SET),
                experiment.getExperimentName(),
                variantId,
                variantName);
    }

    private void setExperimentEntries(ListPreference listPreference, ExperimentConfiguration experiment) {
        List<String> variants = Lists.transform(experiment.getVariants(), variant -> variant.first() + VARIANT_DELIMITER + variant.second().or(Consts.NOT_SET));
        int entriesCount = variants.size();
        CharSequence[] entryLabels = variants.toArray(new CharSequence[entriesCount + 1]);
        CharSequence[] entryValues = variants.toArray(new CharSequence[entriesCount + 1]);

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
