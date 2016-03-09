package com.soundcloud.android.configuration;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

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
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.TryWithBackOff;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.net.HttpHeaders;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ConfigurationOperations {

    public static final String TAG = "Configuration";
    static final long CONFIGURATION_STALE_TIME_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private static final String PARAM_EXPERIMENT_LAYERS = "experiment_layers";

    private static final int POLLING_INITIAL_DELAY = 1;
    private static final int POLLING_INTERVAL_SECONDS = 2;
    private static final int POLLING_MAX_ATTEMPTS = 3;

    private final ApiClientRx apiClientRx;
    private final ApiClient apiClient;
    private final ExperimentOperations experimentOperations;
    private final FeatureOperations featureOperations;
    private final PlanChangeDetector planChangeDetector;
    private final FeatureFlags featureFlags;
    private final ConfigurationSettingsStorage configurationSettingsStorage;
    private final TryWithBackOff<Configuration> tryWithBackOff;
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
            return apiClientRx.mappedResponse(configurationRequestBuilderForGet().build(), Configuration.class);
        }
    };

    private final Action1<Configuration> saveConfiguration = new Action1<Configuration>() {
        @Override
        public void call(Configuration configuration) {
            saveConfiguration(configuration);
        }
    };

    private static Func1<Configuration, Boolean> isExpectedPlan(final Plan plan) {
        return new Func1<Configuration, Boolean>() {
            @Override
            public Boolean call(Configuration configuration) {
                return configuration.userPlan.currentPlan.equals(plan);
            }
        };
    }

    @Inject
    public ConfigurationOperations(ApiClientRx apiClientRx,
                                   ExperimentOperations experimentOperations,
                                   FeatureOperations featureOperations,
                                   FeatureFlags featureFlags,
                                   ConfigurationSettingsStorage configurationSettingsStorage,
                                   TryWithBackOff.Factory tryWithBackOffFactory,
                                   @Named(HIGH_PRIORITY) Scheduler scheduler,
                                   PlanChangeDetector planChangeDetector) {
        this(apiClientRx, experimentOperations, featureOperations, planChangeDetector, featureFlags,
                configurationSettingsStorage, tryWithBackOffFactory.<Configuration>withDefaults(), scheduler);
    }

    @VisibleForTesting
    ConfigurationOperations(ApiClientRx apiClientRx,
                            ExperimentOperations experimentOperations,
                            FeatureOperations featureOperations,
                            PlanChangeDetector planChangeDetector,
                            FeatureFlags featureFlags,
                            ConfigurationSettingsStorage configurationSettingsStorage,
                            TryWithBackOff<Configuration> tryWithBackOff,
                            @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.planChangeDetector = planChangeDetector;
        this.apiClient = apiClientRx.getApiClient();
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
        this.configurationSettingsStorage = configurationSettingsStorage;
        this.tryWithBackOff = tryWithBackOff;
        this.scheduler = scheduler;
    }

    Observable<Configuration> update() {
        return Observable.zip(
                experimentOperations.loadAssignment(),
                fetchConfigurationWithRetry(configurationRequestBuilderForGet().build())
                        .subscribeOn(scheduler)
                        .toObservable(),
                TO_UPDATED_CONFIGURATION
        );
    }

    @NonNull
    private Single<Configuration> fetchConfigurationWithRetry(final ApiRequest request) {
        return Single.defer(new Callable<Single<Configuration>>() {
            @Override
            public Single<Configuration> call() throws Exception {
                return Single.just(tryWithBackOff.call(fetchConfiguration(request)));
            }
        });
    }

    private Callable<Configuration> fetchConfiguration(final ApiRequest request) {
        return new Callable<Configuration>() {
            @Override
            public Configuration call() throws Exception {
                return apiClient.fetchMappedResponse(request, Configuration.class);
            }
        };
    }

    Observable<Configuration> updateIfNecessary() {
        final long now = System.currentTimeMillis();
        if (configurationSettingsStorage.getLastConfigurationCheckTime() < now - CONFIGURATION_STALE_TIME_MILLIS) {
            return update();
        } else {
            Log.d(TAG, "Skipping update; recently updated.");
            return Observable.empty();
        }
    }

    public Observable<Configuration> awaitConfigurationWithPlan(final Plan expectedPlan) {
        return Observable.interval(POLLING_INITIAL_DELAY, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS, scheduler)
                .take(POLLING_MAX_ATTEMPTS)
                .flatMap(toFetchConfiguration)
                .takeFirst(isExpectedPlan(expectedPlan))
                .doOnNext(saveConfiguration);
    }

    public Observable<Configuration> awaitConfigurationFromPendingPlanChange() {
        //TODO: account for mid-tier
        if (isPendingHighTierUpgrade()) {
            return awaitConfigurationWithPlan(configurationSettingsStorage.getPendingPlanUpgrade());
        } else if (isPendingDowngrade()) {
            return awaitConfigurationWithPlan(configurationSettingsStorage.getPendingPlanDowngrade());
        } else {
            return Observable.error(new IllegalStateException("Expected a pending plan change, but none found."));
        }
    }

    public DeviceManagement registerDevice(Token token) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Registering device");
        final ApiRequest request = configurationRequestBuilderForGet()
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token)).build();

        Configuration configuration = apiClient.fetchMappedResponse(request, Configuration.class);
        saveConfiguration(configuration);
        return configuration.deviceManagement;
    }

    public DeviceManagement forceRegisterDevice(Token token) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Forcing device registration");
        final ApiRequest request = ApiRequest.post(ApiEndpoints.CONFIGURATION.path())
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token))
                .forPrivateApi(1)
                .build();

        return apiClient.fetchMappedResponse(request, Configuration.class).deviceManagement;
    }

    public Observable<Object> deregisterDevice() {
        return apiClientRx.response(ApiRequest.delete(ApiEndpoints.CONFIGURATION.path())
                .forPrivateApi(1)
                .build())
                .doOnNext(new Action1<ApiResponse>() {
                    @Override
                    public void call(ApiResponse apiResponse) {
                        Log.d(TAG, "De-registered device");
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ErrorUtils.handleThrowable(throwable, ConfigurationOperations.class);
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
        Log.d(TAG, "Saving new configuration...");
        configurationSettingsStorage.setLastConfigurationUpdateTime(System.currentTimeMillis());
        experimentOperations.update(configuration.assignment);
        if (featureFlags.isEnabled(Flag.SOUNDCLOUD_GO)) {
            featureOperations.updateFeatures(configuration.features);
            planChangeDetector.handleRemotePlan(configuration.userPlan.currentPlan);
            featureOperations.updatePlan(configuration.userPlan);
        }
    }

    public boolean isPendingHighTierUpgrade() {
        return configurationSettingsStorage.getPendingPlanUpgrade() == Plan.HIGH_TIER;
    }

    public boolean isPendingDowngrade() {
        return configurationSettingsStorage.getPendingPlanDowngrade() != Plan.UNDEFINED;
    }

    public void clearPendingPlanChanges() {
        configurationSettingsStorage.clearPendingPlanChanges();
    }

    public void clearConfigurationSettings() {
        configurationSettingsStorage.clear();
    }
}
