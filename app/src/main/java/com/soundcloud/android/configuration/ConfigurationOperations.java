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
import com.soundcloud.android.image.ImageConfigurationStorage;
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
import rx.functions.Func0;
import rx.functions.Func1;

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
    private final ForceUpdateHandler forceUpdateHandler;
    private final ConfigurationSettingsStorage configurationSettingsStorage;
    private final ImageConfigurationStorage imageConfigurationStorage;
    private final TryWithBackOff<Configuration> tryWithBackOff;
    private final Scheduler scheduler;

    private final Func1<Long, Observable<Configuration>> toFetchConfiguration =
            new Func1<Long, Observable<Configuration>>() {
                @Override
                public Observable<Configuration> call(Long tick) {
                    return fetchConfigurationWithRetry(configurationRequestBuilderForGet().build()).toObservable();
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
                return configuration.getUserPlan().currentPlan.equals(plan);
            }
        };
    }

    @Inject
    public ConfigurationOperations(ApiClientRx apiClientRx,
                                   ExperimentOperations experimentOperations,
                                   FeatureOperations featureOperations,
                                   ConfigurationSettingsStorage configurationSettingsStorage,
                                   TryWithBackOff.Factory tryWithBackOffFactory,
                                   @Named(HIGH_PRIORITY) Scheduler scheduler,
                                   PlanChangeDetector planChangeDetector,
                                   ForceUpdateHandler forceUpdateHandler,
                                   ImageConfigurationStorage imageConfigurationStorage) {
        this(apiClientRx, experimentOperations, featureOperations, planChangeDetector, forceUpdateHandler,
             configurationSettingsStorage, imageConfigurationStorage,
             tryWithBackOffFactory.<Configuration>withDefaults(), scheduler);
    }

    @VisibleForTesting
    ConfigurationOperations(ApiClientRx apiClientRx,
                            ExperimentOperations experimentOperations,
                            FeatureOperations featureOperations,
                            PlanChangeDetector planChangeDetector,
                            ForceUpdateHandler forceUpdateHandler,
                            ConfigurationSettingsStorage configurationSettingsStorage,
                            ImageConfigurationStorage imageConfigurationStorage,
                            TryWithBackOff<Configuration> tryWithBackOff,
                            @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.planChangeDetector = planChangeDetector;
        this.forceUpdateHandler = forceUpdateHandler;
        this.imageConfigurationStorage = imageConfigurationStorage;
        this.apiClient = apiClientRx.getApiClient();
        this.experimentOperations = experimentOperations;
        this.featureOperations = featureOperations;
        this.configurationSettingsStorage = configurationSettingsStorage;
        this.tryWithBackOff = tryWithBackOff;
        this.scheduler = scheduler;
    }

    Observable<Configuration> update() {
        return Observable.defer(new Func0<Observable<Configuration>>() {
            @Override
            public Observable<Configuration> call() {
                experimentOperations.loadAssignment();
                return fetchConfigurationWithRetry(configurationRequestBuilderForGet().build())
                        .subscribeOn(scheduler)
                        .toObservable();
            }
        });
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
            // this might seem counter-intuitive since it actually violates a precondition, but we actually
            // found a case where due to abnormal termination of the offboarding activity, Android launched
            // us back into it after we successfully completed the flow, so instead of throwing an error
            // here we just nod and move on.
            // See https://github.com/soundcloud/SoundCloud-Android/issues/5024
            return Observable.empty();
        }
    }

    public DeviceManagement registerDevice(Token token) throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Registering device");
        final ApiRequest request = configurationRequestBuilderForGet()
                .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token)).build();

        Configuration configuration = apiClient.fetchMappedResponse(request, Configuration.class);
        saveConfiguration(configuration);
        return configuration.getDeviceManagement();
    }

    public DeviceManagement forceRegisterDevice(Token token)
            throws ApiRequestException, IOException, ApiMapperException {
        Log.d(TAG, "Forcing device registration");
        final ApiRequest request = ApiRequest.post(ApiEndpoints.CONFIGURATION.path())
                                             .withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(token))
                                             .forPrivateApi()
                                             .build();

        return apiClient.fetchMappedResponse(request, Configuration.class).getDeviceManagement();
    }

    public Observable<Object> deregisterDevice() {
        return apiClientRx.response(ApiRequest.delete(ApiEndpoints.DEVICE_REGISTRATION.path())
                                              .forPrivateApi()
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
                         .forPrivateApi();
    }

    void saveConfiguration(Configuration configuration) {
        Log.d(TAG, "Saving new configuration...");
        configurationSettingsStorage.setLastConfigurationUpdateTime(System.currentTimeMillis());

        forceUpdateHandler.checkForForcedUpdate(configuration);

        experimentOperations.update(configuration.getAssignment());
        imageConfigurationStorage.storeAvailableSizeSpecs(configuration.getImageSizeSpecs());

        featureOperations.updateFeatures(configuration.getFeatures());
        planChangeDetector.handleRemotePlan(configuration.getUserPlan().currentPlan);
        featureOperations.updatePlan(configuration.getUserPlan());
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