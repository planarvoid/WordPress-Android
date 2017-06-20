package com.soundcloud.android.properties;

import com.soundcloud.android.Consts;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;

import android.content.Context;

/**
 * Used only for testing.
 */
@VisibleForTesting
public class ExperimentsHelper {

    private final ExperimentOperations experimentOperations;

    private ExperimentsHelper(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public static ExperimentsHelper create(Context context) {
        return new ExperimentsHelper(SoundCloudTestApplication.fromContext(context).getExperimentOperations());
    }

    public void set(ExperimentConfiguration configuration, String variant) {
        experimentOperations.forceExperimentVariation(buildExperimentLayer(configuration, variant));
    }

    private Layer buildExperimentLayer(ExperimentConfiguration experiment, String variant) {
        Pair<String, Optional<Integer>> variantData = getVariantData(experiment, variant);

        final String variantName = variantData.first();
        final int variantId = variantData.second().or(Consts.NOT_SET);

        return new Layer(
                experiment.getLayerName(),
                experiment.getExperimentId().or(Consts.NOT_SET),
                experiment.getExperimentName(),
                variantId,
                variantName);
    }

    private Pair<String, Optional<Integer>> getVariantData(ExperimentConfiguration experiment, String variant) {
        for (Pair<String, Optional<Integer>> pair : experiment.getVariants()) {
            if (variant.equalsIgnoreCase(pair.first())) {
                return pair;
            }
        }
        throw new IllegalArgumentException("Variant " + variant + " not found for experiment " + experiment.getExperimentName());
    }
}
