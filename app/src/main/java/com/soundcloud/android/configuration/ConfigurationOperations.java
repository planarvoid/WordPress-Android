package com.soundcloud.android.configuration;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.java.net.HttpHeaders;
import dagger.Lazy;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConfigurationOperations {

    private static final String TAG = "Configuration";
    private static final String PARAM_EXPERIMENT_LAYERS = "experiment_layers";

    private static final int POLLING_INITIAL_DELAY = 1;
    private static final int POLLING_INTERVAL_SECONDS = 2;
    private static final int POLLING_MAX_ATTEMPTS = 3;

    private final Lazy<ApiClientRx> apiClientRx;
    private final Lazy<ApiClient> apiClient;
    private final ExperimentOperations experimentOperations;
    private final FeatureOperations featureOperations;
    private final FeatureFlags featureFlags;
    private final Scheduler scheduler;

    private static final Func2<Object, Configuration, Configuration> TO_UPDATED_CONFIGURATION = new Func2<Object, Configuration, Configuration>() {
        @Override
        public Configuration call(Object ignore, Configuration configuration) {
            return configuration;
        }
    };

    private final Func1<Long, Observable<Configuration>> toFetchConfiguration = new Func1<Long, Observable<Configuration>>() {
        @Override
        public Observable<Configuration> call(Long tick) {
            return apiClientRx.get().mappedResponse(configurationRequestBuilderForGet().build(), Configuration.class);
        }
    };

    private final Action1<Configuration> saveConfiguration = new Action1<Configuration>() {
        @Override
        public void call(Configuration configuration) {
            saveConfiguration(configuration);
        }
    };

    private static Func1<Configuration, Boolean> isExpectedPlan(final String planId) {
        return new Func1<Configuration, Boolean>() {
            @Override
            public Boolean call(Configuration configuration) {
                return configuration.plan.id.equals(planId);
            }
        };
    }

    @Inject
    public ConfigurationOperations(Lazy<ApiClientRx> apiClientRx, Lazy<ApiClient> apiClient,
                                   ExperimentOperations experimentOperations, FeatureOperations featureOperations,
                                   FeatureFlags featureFlags, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.apiClient = apiClient;
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
        this.scheduler = scheduler;
    }

    Observable<Configuration> update() {
        final ApiRequest request = configurationRequestBuilderForGet().build();
        return Observable.zip(experimentOperations.loadAssignment(),
                apiClientRx.get().mappedResponse(request, Configuration.class).subscribeOn(scheduler),
                TO_UPDATED_CONFIGURATION
        );
    }

    public Observable<Configuration> awaitConfigurationWithPlan(final String expectedPlan) {
        return Observable.interval(POLLING_INITIAL_DELAY, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS, scheduler)
                .take(POLLING_MAX_ATTEMPTS)
                .flatMap(toFetchConfiguration)
                .first(isExpectedPlan(expectedPlan)).doOnNext(saveConfiguration);
    }

    public DeviceManagement registerDevice(Token token) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Registering device");
        final ApiRequest request = configurationRequestBuilderForGet()
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token)).build();

        Configuration configuration = apiClient.get().fetchMappedResponse(request, Configuration.class);
        saveConfiguration(configuration);
        return configuration.deviceManagement;
    }

    public DeviceManagement forceRegisterDevice(Token token, String deviceIdToDeregister) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Forcing device registration");
        final Map<String, Map<String, String>> content = Collections.singletonMap("conflicting_device", Collections.singletonMap("device_id", deviceIdToDeregister));
        final ApiRequest request = ApiRequest.post(ApiEndpoints.CONFIGURATION.path())
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token))
                .withContent(content)
                .forPrivateApi(1)
                .build();

        return apiClient.get().fetchMappedResponse(request, Configuration.class).deviceManagement;
    }

    public Observable<Object> deregisterDevice() {
        return apiClientRx.get().response(ApiRequest.delete(ApiEndpoints.CONFIGURATION.path())
                .forPrivateApi(1)
                .build())
                .doOnNext(new Action1<ApiResponse>() {
                    @Override
                    public void call(ApiResponse apiResponse) {
                        Log.d(TAG, "De-registered device");
                    }
                })
                .cast(Object.class)
                .onErrorResumeNext(Observable.just(RxUtils.EMPTY_VALUE));
    }

    private ApiRequest.Builder configurationRequestBuilderForGet() {
        return ApiRequest.get(ApiEndpoints.CONFIGURATION.path())
                .addQueryParam(PARAM_EXPERIMENT_LAYERS, experimentOperations.getActiveLayers())
                .forPrivateApi(1);
    }

    void saveConfiguration(Configuration configuration) {
        experimentOperations.update(configuration.assignment);
        if (featureFlags.isEnabled(Flag.OFFLINE_SYNC)) {
            featureOperations.updateFeatures(configuration.features);
            featureOperations.updatePlan(configuration.plan.id, configuration.plan.upsells);
        }
    }

}
