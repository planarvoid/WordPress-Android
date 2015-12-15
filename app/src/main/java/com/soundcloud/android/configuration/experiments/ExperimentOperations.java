package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.Consts;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ExperimentOperations {

    private static final String EXPERIMENT_PREFIX = "exp_";

    private final Action1<Assignment> cacheCurrentAssignments = new Action1<Assignment>() {
        @Override
        public void call(Assignment assignment) {
            ExperimentOperations.this.assignment = assignment;
        }
    };

    private final ExperimentStorage experimentStorage;
    private final ActiveExperiments activeExperiments;

    private Assignment assignment = Assignment.empty();

    @Inject
    public ExperimentOperations(ExperimentStorage experimentStorage, ActiveExperiments activeExperiments) {
        this.experimentStorage = experimentStorage;
        this.activeExperiments = activeExperiments;
    }

    public Observable<Assignment> loadAssignment() {
        return experimentStorage.readAssignment()
                .doOnNext(cacheCurrentAssignments);
    }

    public String[] getActiveLayers() {
        return activeExperiments.getRequestLayers();
    }

    public void update(Assignment assignment) {
        experimentStorage.storeAssignment(assignment);
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public String getExperimentVariant(String experiment) {
        for (Layer layer : assignment.getLayers()) {
            if (experiment.equals(layer.getExperimentName())) {
                return layer.getVariantName();
            }
        }
        return Strings.EMPTY;
    }

    public Map<String, Integer> getTrackingParams() {
        HashMap<String, Integer> params = new HashMap<>();
        for (Layer layer : assignment.getLayers()) {
            if (activeExperiments.isActive(layer.getExperimentName())) {
                params.put(EXPERIMENT_PREFIX + layer.getLayerName(), layer.getVariantId());
            }
        }
        return params;
    }

    public void forceExperimentVariation(Experiment experiment, String variation) {
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

    private Layer buildExperimentLayer(Experiment experiment, String variation) {
        Layer layer = new Layer();
        layer.setLayerName(experiment.getLayerName());
        layer.setExperimentName(experiment.getName());
        layer.setVariantName(variation);
        layer.setVariantId(Consts.NOT_SET);
        return layer;
    }
}
