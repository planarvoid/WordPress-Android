package com.soundcloud.android.configuration;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.WriteResult;
import dagger.Lazy;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import android.util.Log;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ConfigurationOperations {

    private static final String TAG = "Configuration";
    private static final String PARAM_EXPERIMENT_LAYERS = "experiment_layers";

    private final Lazy<ApiScheduler> apiScheduler;
    private final Lazy<ApiClient> apiClient;
    private final ExperimentOperations experimentOperations;
    private final FeatureOperations featureOperations;
    private final AccountOperations accountOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final DeviceManagementStorage deviceManagementStorage;
    private final FeatureFlags featureFlags;

    private Subscription subscription = Subscriptions.empty();

    private final Func2<Object, Configuration, Configuration> toUpdatedConfiguration = new Func2<Object, Configuration, Configuration>() {
        @Override
        public Configuration call(Object ignore, Configuration configuration) {
            return configuration;
        }
    };

    private final Func1<Void, Observable<WriteResult>> clearOfflineContent = new Func1<Void, Observable<WriteResult>>() {
        @Override
        public Observable<WriteResult> call(Void ignore) {
            return offlineContentOperations.clearOfflineContent();
        }
    };

    @Inject
    public ConfigurationOperations(Lazy<ApiScheduler> apiScheduler, Lazy<ApiClient> apiClient, ExperimentOperations experimentOperations,
                                   FeatureOperations featureOperations, AccountOperations accountOperations,
                                   OfflineContentOperations offlineContentOperations, DeviceManagementStorage deviceManagementStorage,
                                   FeatureFlags featureFlags) {
        this.apiScheduler = apiScheduler;
        this.apiClient = apiClient;
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.accountOperations = accountOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.deviceManagementStorage = deviceManagementStorage;
        this.featureFlags = featureFlags;
    }

    public void update() {
        Log.d(TAG, "Requesting configuration");
        subscription.unsubscribe();
        subscription = loadAndUpdateConfiguration().subscribe(new ConfigurationSubscriber());
    }

    public DeviceManagement forceRegisterDevice(Token token, String deviceIdToDeregister) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Forcing device registration");
        final Map<String, Map<String, String>> content = Collections.singletonMap("conflicting_device", Collections.singletonMap("device_id", deviceIdToDeregister));
        final ApiRequest<Configuration> request = ApiRequest.Builder.<Configuration>post(ApiEndpoints.CONFIGURATION.path())
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token))
                .withContent(content)
                .forPrivateApi(1)
                .forResource(Configuration.class).build();

        return apiClient.get().fetchMappedResponse(request).deviceManagement;
    }

    public DeviceManagement registerDevice(Token token) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Registering device");
        final ApiRequest<Configuration> request = getConfigurationRequestBuilderForGet()
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token)).build();

        Configuration configuration = apiClient.get().fetchMappedResponse(request);
        saveConfiguration(configuration);
        return configuration.deviceManagement;
    }

    public boolean shouldDisplayDeviceConflict() {
        return deviceManagementStorage.hadDeviceConflict();
    }

    public void clearDeviceConflict() {
        deviceManagementStorage.clearDeviceConflict();
    }

    private Observable<Configuration> loadAndUpdateConfiguration() {
        final ApiRequest<Configuration> request = getConfigurationRequestBuilderForGet().build();
        return Observable.zip(experimentOperations.loadAssignment(),
                apiScheduler.get().mappedResponse(request),
                toUpdatedConfiguration
        );
    }

    private ApiRequest.Builder<Configuration> getConfigurationRequestBuilderForGet() {
        return ApiRequest.Builder.<Configuration>get(ApiEndpoints.CONFIGURATION.path())
                .addQueryParam(PARAM_EXPERIMENT_LAYERS, experimentOperations.getActiveLayers())
                .forPrivateApi(1)
                .forResource(Configuration.class);
    }

    private class ConfigurationSubscriber extends DefaultSubscriber<Configuration> {
        @Override
        public void onNext(Configuration configuration) {
            Log.d(TAG, "Received new configuration");
            if (configuration.deviceManagement.isNotAuthorized()) {
                Log.d(TAG, "Unauthorized device, logging out");
                deviceManagementStorage.setDeviceConflict();
                fireAndForget(accountOperations.logout().flatMap(clearOfflineContent));
            }
            saveConfiguration(configuration);
        }

    }

    public void saveConfiguration(Configuration configuration) {
        experimentOperations.update(configuration.assignment);
        if (featureFlags.isEnabled(Flag.CONFIGURATION_FEATURES)) {
            featureOperations.update(configuration.getFeatureMap());
        }
    }

}
