package com.soundcloud.android.configuration;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import android.util.Log;

import javax.inject.Inject;

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
    private final DeviceHelper deviceHelper;
    private final FeatureFlags featureFlags;
    private  Subscription subscription = Subscriptions.empty();

    @Inject
    public ConfigurationOperations(ApiScheduler apiScheduler, ExperimentOperations experimentOperations,
                                   FeatureOperations featureOperations, DeviceHelper deviceHelper,
                                   FeatureFlags featureFlags) {
        this.apiScheduler = apiScheduler;
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.deviceHelper = deviceHelper;
        this.featureFlags = featureFlags;
    }

    public void update() {
        Log.d(TAG, "Requesting configuration for device: " + deviceHelper.getUDID());
        subscription.unsubscribe();
        subscription = loadAndUpdateConfiguration().subscribe(new StoreConfigurationSubscriber());
    }

    private Observable<Configuration> loadAndUpdateConfiguration() {
        final ApiRequest<Configuration> request = addDeviceIdIfAny(ApiRequest.Builder.<Configuration>get(ApiEndpoints.CONFIGURATION.path())
                .addQueryParam(PARAM_EXPERIMENT_LAYERS, experimentOperations.getActiveLayers())
                .forPrivateApi(1)
                .forResource(Configuration.class))
                .build();

        return Observable.zip(experimentOperations.loadAssignment(),
                apiScheduler.mappedResponse(request),
                toUpdatedConfiguration
        );
    }

    private ApiRequest.Builder<Configuration> addDeviceIdIfAny(ApiRequest.Builder<Configuration> builder) {
        if (ScTextUtils.isNotBlank(deviceHelper.getUDID())) {
            builder.withHeader(ApiRequest.HEADER_UDID, deviceHelper.getUDID());
        }
        return builder;
    }

    private class StoreConfigurationSubscriber extends DefaultSubscriber<Configuration> {
        @Override
        public void onNext(Configuration configuration) {
            Log.d(TAG, "Received new configuration");
            experimentOperations.update(configuration.assignment);
            if (featureFlags.isEnabled(Flag.CONFIGURATION_FEATURES)) {
                featureOperations.update(configuration.features);
            }
        }
    }
}
