package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.Consts;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ExperimentOperations {

    private final ExperimentStorage experimentStorage;
    private final ActiveExperiments activeExperiments;

    private Assignment assignment;

    @Inject
    public ExperimentOperations(ExperimentStorage experimentStorage, ActiveExperiments activeExperiments) {
        this.experimentStorage = experimentStorage;
        this.activeExperiments = activeExperiments;
        loadAssignment();
    }

    public String[] getActiveLayers() {
        return activeExperiments.getRequestLayers();
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
            if (experiment.matches(layer)) {
                return Optional.of(layer);
            }
        }
        return Optional.absent();
    }

    public ArrayList<Integer> getActiveVariants() {

        ArrayList<Integer> activeVariants = new ArrayList<>();

        for (Layer layer : assignment.getLayers()) {
            if (activeExperiments.isActive(layer)) {
                activeVariants.add(layer.getVariantId());
            }
        }

        return activeVariants;
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
