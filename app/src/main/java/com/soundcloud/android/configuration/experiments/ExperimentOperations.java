package com.soundcloud.android.configuration.experiments;

import rx.Observable;
import rx.functions.Action1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ExperimentOperations {

    private static final String TAG = "Configuration";
    private static final String EXPERIMENT_PREFIX = "exp_";

    private final Action1<Assignment> cacheCurrentAssignments = new Action1<Assignment>() {
        @Override
        public void call(Assignment assignment) {
            Log.d(TAG, "Loaded current experiment config\n" + assignment.toString());
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
        Log.d(TAG, "Store next experiment config\n" + assignment.toString());
        experimentStorage.storeAssignment(assignment);
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Map<String, Integer> getTrackingParams() {
        HashMap<String, Integer> params = new HashMap<>();
        for (Layer layer : assignment.getLayers()) {
            if (activeExperiments.isActive(layer.getExperimentId())) {
                params.put(EXPERIMENT_PREFIX + layer.getLayerName(), layer.getVariantId());
            }
        }
        return params;
    }
}
