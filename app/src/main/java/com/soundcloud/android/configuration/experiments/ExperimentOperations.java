package com.soundcloud.android.configuration.experiments;

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

    public Optional<String> getOptionalExperimentVariant(ExperimentConfiguration experiment) {
        return findLayer(experiment).transform(Layer::getVariantName);
    }

    Optional<Layer> findLayer(ExperimentConfiguration experiment) {
        for (Layer layer : assignment.getLayers()) {
            if (matches(experiment, layer)) {
                return Optional.of(layer);
            }
        }
        return Optional.absent();
    }

    /**
     * @return active experiments separated by commas, absent if development mode
     */
    public Optional<String> getSerializedActiveVariants() {
        return applicationProperties.isDevelopmentMode() ? Optional.absent() : assignment.commaSeparatedVariantIds();
    }

    @VisibleForTesting
    static boolean matches(ExperimentConfiguration experimentConfiguration, Layer layer) {
        if (experimentConfiguration.getLayerName().equals(layer.getLayerName())) {
            if (experimentConfiguration.getExperimentId().isPresent()) {
                return experimentConfiguration.getExperimentId().get().equals(layer.getExperimentId());
            } else {
                return experimentConfiguration.getExperimentName().equals(layer.getExperimentName());
            }
        } else {
            return false;
        }
    }

    public void forceExperimentVariation(Layer layer) {
        List<Layer> existingLayers = assignment.getLayers();
        List<Layer> newLayers = new ArrayList<>(existingLayers.size() + 1);

        for (Layer existingLayer : existingLayers) {
            if (!existingLayer.getLayerName().equals(layer.getLayerName())) {
                newLayers.add(existingLayer);
                break;
            }
        }

        newLayers.add(layer);
        assignment = new Assignment(newLayers);
        update(assignment);
    }
}
