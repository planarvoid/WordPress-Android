package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.functions.Action1;

import javax.annotation.Nullable;
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

    @Nullable private Assignment assignment = null;

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
        // TODO : Refactor this to remove the from the Operations.
        // This fixes a race condition. The app may or may not apply
        // the variant when the user has been assigned.
        if (assignment == null) {
            try {
                assignment = loadAssignment().toBlocking().first();
            } catch (Exception e) {
                assignment = Assignment.empty();
                ErrorUtils.handleSilentException(e);
                e.printStackTrace();
            }
        }
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

    public Optional<Layer> findLayer(ExperimentConfiguration experiment) {
        for (Layer layer : getAssignment().getLayers()) {
            if (experiment.matches(layer)) {
                return Optional.of(layer);
            }
        }
        return Optional.absent();
    }

    public Map<String, Integer> getTrackingParams() {
        HashMap<String, Integer> params = new HashMap<>();
        for (Layer layer : getAssignment().getLayers()) {
            if (activeExperiments.isActive(layer)) {
                params.put(EXPERIMENT_PREFIX + layer.getLayerName(), layer.getVariantId());
            }
        }
        return params;
    }

    public void forceExperimentVariation(ExperimentConfiguration experiment, String variation) {
        List<Layer> existingLayers = getAssignment().getLayers();
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
