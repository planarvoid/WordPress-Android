package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.Consts;
import com.soundcloud.android.experiments.ActiveExperiments;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ExperimentOperations {

    static final String LISTENING_LAYER = "android_listening";
    private static final String[] ACTIVE_LAYERS = {LISTENING_LAYER};

    private final ExperimentStorage experimentStorage;
    private final ApplicationProperties applicationProperties;

    private Assignment assignment;

    @Inject
    public ExperimentOperations(ExperimentStorage experimentStorage, ApplicationProperties applicationProperties) {
        this.experimentStorage = experimentStorage;
        this.applicationProperties = applicationProperties;
        loadAssignment();
    }

    public String[] getActiveLayers() {
        return ACTIVE_LAYERS;
    }

    public void update(Assignment updatedAssignment) {
        assignment = updatedAssignment;
        experimentStorage.storeAssignment(updatedAssignment);
    }

    void loadAssignment() {
        this.assignment = experimentStorage.readAssignment();
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public String getExperimentVariant(ExperimentConfiguration experiment) {
        final Optional<Layer> layer = findLayer(experiment);
        if (layer.isPresent()) {
            return layer.get().getVariantName();
        } else {
            return Strings.EMPTY;
        }
    }

    Optional<Layer> findLayer(ExperimentConfiguration experiment) {
        for (Layer layer : assignment.getLayers()) {
            if (matches(experiment, layer)) {
                return Optional.of(layer);
            }
        }
        return Optional.absent();
    }

    public ArrayList<Integer> getActiveVariants() {

        ArrayList<Integer> activeVariants = new ArrayList<>();

        for (Layer layer : assignment.getLayers()) {
            if (isActive(layer)) {
                activeVariants.add(layer.getVariantId());
            }
        }

        return activeVariants;
    }

    private boolean isActive(Layer layer) {
        if (!applicationProperties.isDevelopmentMode()) {
            for (ExperimentConfiguration experiment : ActiveExperiments.ACTIVE_EXPERIMENTS) {
                if (matches(experiment, layer)) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    static boolean matches(ExperimentConfiguration experimentConfiguration, Layer layer) {
        if (layer.getLayerName().equals(experimentConfiguration.getLayerName())) {
            if (experimentConfiguration.isPattern()) {
                return layer.getExperimentName().matches(experimentConfiguration.getName());
            } else {
                return layer.getExperimentName().equals(experimentConfiguration.getName());
            }
        }
        return false;
    }

    public void forceExperimentVariation(ExperimentConfiguration experiment, String variation) {
        List<Layer> existingLayers = assignment.getLayers();
        List<Layer> newLayers = new ArrayList<>(existingLayers.size() + 1);
        String layerName = experiment.getLayerName();

        for (Layer existingLayer : existingLayers) {
            if (!existingLayer.getLayerName().equals(layerName)) {
                newLayers.add(existingLayer);
                break;
            }
        }

        newLayers.add(buildExperimentLayer(experiment, variation));
        assignment = new Assignment(newLayers);
        update(assignment);
    }

    private Layer buildExperimentLayer(ExperimentConfiguration experiment, String variation) {
        return new Layer(
                experiment.getLayerName(),
                Consts.NOT_SET,
                experiment.getName(),
                Consts.NOT_SET,
                variation);
    }
}
