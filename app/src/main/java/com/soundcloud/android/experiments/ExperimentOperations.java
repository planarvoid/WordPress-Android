package com.soundcloud.android.experiments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DeviceHelper;
import rx.functions.Action0;
import rx.functions.Action1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ExperimentOperations {

    private static final String TAG = ExperimentOperations.class.getSimpleName();

    private static final String PARAM_LAYERS = "layers";
    private static final String EXPERIMENT_PREFIX = "exp_";

    private Assignment assignment = Assignment.empty();

    private final ExperimentStorage experimentStorage;
    private final ApiScheduler apiScheduler;
    private final ActiveExperiments activeExperiments;
    private final DeviceHelper deviceHelper;

    private final Action1 storeAssignment = new Action1<Assignment>() {
        @Override
        public void call(Assignment assignment) {
            experimentStorage.storeAssignment(assignment);
        }
    };

    @Inject
    public ExperimentOperations(ExperimentStorage experimentStorage, ApiScheduler apiScheduler,
                                ActiveExperiments activeExperiments, DeviceHelper deviceHelper) {
        this.experimentStorage = experimentStorage;
        this.apiScheduler = apiScheduler;
        this.activeExperiments = activeExperiments;
        this.deviceHelper = deviceHelper;
    }

    public void loadAssignment() {
        String deviceId = deviceHelper.getUniqueDeviceID();
        if(isNotBlank(deviceId)) {
            experimentStorage.loadAssignmentAsync()
                    .finallyDo(fetchAndStoreAssignment(deviceId))
                    .subscribe(new LoadSubscriber());
        }
    }

    @VisibleForTesting
    Assignment getAssignment() {
        return assignment;
    }

    public Map<String, Integer> getTrackingParams() {
        HashMap<String, Integer> params = new HashMap<String, Integer>();
        for (Layer layer : assignment.getLayers()) {
            if (activeExperiments.isActive(layer.getExperimentId())) {
                params.put(EXPERIMENT_PREFIX + layer.getLayerName(), layer.getVariantId());
            }
        }
        return params;
    }

    private Action0 fetchAndStoreAssignment(final String deviceId) {
        return new Action0() {
            @Override
            public void call() {
                Log.d(TAG, "Requesting assignments for device: " + deviceId);
                ApiRequest<Assignment> request =
                        ApiRequest.Builder.<Assignment>get(ApiEndpoints.EXPERIMENTS.path(deviceId))
                                .addQueryParam(PARAM_LAYERS, activeExperiments.getRequestLayers())
                                .forPrivateApi(1)
                                .forResource(TypeToken.of(Assignment.class))
                                .build();
                fireAndForget(apiScheduler.mappedResponse(request).doOnNext(storeAssignment));
            }
        };
    }

    private final class LoadSubscriber extends DefaultSubscriber<Assignment> {

        @Override
        public void onNext(Assignment assignment) {
            ExperimentOperations.this.assignment = assignment;
            Log.d(TAG, assignment.toString());
        }
    }

}
