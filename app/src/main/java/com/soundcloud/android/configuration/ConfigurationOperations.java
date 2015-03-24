package com.soundcloud.android.configuration;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import android.util.Log;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationOperations {

    private static final String TAG = "Configuration";
    private static final String PARAM_EXPERIMENT_LAYERS = "experiment_layers";

    private final Func2<Object, Configuration, Configuration> toUpdatedConfiguration = new Func2<Object, Configuration, Configuration>() {
        @Override
        public Configuration call(Object ignore, Configuration configuration) {
            return configuration;
        }
    };

    private final ApiScheduler apiScheduler;
    private final ExperimentOperations experimentOperations;
    private final FeatureOperations featureOperations;
    private final FeatureFlags featureFlags;
    private Subscription subscription = Subscriptions.empty();

    @Inject
    public ConfigurationOperations(ApiScheduler apiScheduler, ExperimentOperations experimentOperations,
                                   FeatureOperations featureOperations, FeatureFlags featureFlags) {
        this.apiScheduler = apiScheduler;
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
    }

    public void update() {
        Log.d(TAG, "Requesting configuration");
        subscription.unsubscribe();
        subscription = loadAndUpdateConfiguration().subscribe(new StoreConfigurationSubscriber());
    }

    private Observable<Configuration> loadAndUpdateConfiguration() {
        final ApiRequest<Configuration> request = ApiRequest.Builder.<Configuration>get(ApiEndpoints.CONFIGURATION.path())
                .addQueryParam(PARAM_EXPERIMENT_LAYERS, experimentOperations.getActiveLayers())
                .forPrivateApi(1)
                .forResource(Configuration.class)
                .build();

        return Observable.zip(experimentOperations.loadAssignment(),
                apiScheduler.mappedResponse(request),
                toUpdatedConfiguration
        );
    }

    private class StoreConfigurationSubscriber extends DefaultSubscriber<Configuration> {
        @Override
        public void onNext(Configuration configuration) {
            Log.d(TAG, "Received new configuration");
            experimentOperations.update(configuration.assignment);

            if (featureFlags.isEnabled(Flag.CONFIGURATION_FEATURES)) {
                featureOperations.update(toMap(configuration));
            }
        }

        private Map<String, Boolean> toMap(Configuration configuration) {
            final HashMap<String, Boolean> map = new HashMap<>(configuration.features.size());
            for (Feature feature : configuration.features) {
                map.put(feature.name, feature.enabled);
            }
            return map;
        }
    }
}
