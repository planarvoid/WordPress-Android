package com.soundcloud.android.configuration;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import com.soundcloud.java.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ConfigurationOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRx apiClientRx;
    @Mock private ApiClient apiClient;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private AccountOperations accountOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private DeviceManagementStorage deviceManagementStorage;

    private ConfigurationOperations operations;
    private Configuration configuration;

    private TestSubscriber<Configuration> subscriber;
    private TestScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        subscriber = new TestSubscriber<>();
        scheduler = new TestScheduler();

        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(InjectionSupport.lazyOf(apiClientRx), InjectionSupport.lazyOf(apiClient),
                experimentOperations, featureOperations, featureFlags, scheduler);


        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(true);
    }

    @Test
    public void updateReturnsConfiguration() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.update().subscribe(subscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertThat(subscriber.getOnNextEvents().get(0)).isSameAs(noPlan);
        subscriber.assertCompleted();
    }

    @Test
    public void updateUntilPlanChangedReturnsConfiguration() {
        final Configuration noPlan = getNoPlanConfiguration();
        final Configuration withPlan = getAuthorizedConfiguration();
        when(featureOperations.getPlan()).thenReturn(noPlan.plan.id);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(withPlan));

        operations.updateUntilPlanChanged().subscribe(subscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(subscriber.getOnNextEvents().get(0)).isSameAs(withPlan);
        subscriber.assertCompleted();
    }

    @Test
    public void updateUntilPlanChangedStopsPollingWhenPlanIsReturned() {
        final Configuration noPlan = getNoPlanConfiguration();
        final Configuration withPlan = getAuthorizedConfiguration();
        when(featureOperations.getPlan()).thenReturn(noPlan.plan.id);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan), Observable.just(withPlan));

        operations.updateUntilPlanChanged().subscribe(subscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        subscriber.assertNoValues();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        subscriber.assertValueCount(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isSameAs(withPlan);
        subscriber.assertCompleted();
    }

    @Test
    public void updateUntilPlanChangedStopsPollingAfterThreeAttempts() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(featureOperations.getPlan()).thenReturn(noPlan.plan.id);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.updateUntilPlanChanged().subscribe(subscriber);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update().subscribe(subscriber);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")), eq(Configuration.class));
    }

    @Test
    public void registerDeviceGetsConfigurationAndReturnsDeviceManagement() throws ApiRequestException, IOException, ApiMapperException {
        Token token = new Token("accessToken","refreshToken");
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                        .withQueryParam("experiment_layers", "android_listening", "ios")
                        .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")),
                eq(Configuration.class))).thenReturn(configuration);

        assertThat(operations.registerDevice(token)).isSameAs(configuration.deviceManagement);
    }

    @Test
    public void registerDeviceStoresConfiguration() throws Exception {
        Token token = new Token("accessToken","refreshToken");
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")
                .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")), eq(Configuration.class))).thenReturn(configuration);

        operations.registerDevice(token);

        verify(featureOperations).updateFeatures(TestFeatures.asList());
        verify(featureOperations).updatePlan(configuration.plan.id, configuration.plan.upsells);
        verify(experimentOperations).update(configuration.assignment);
    }

    @Test
    public void forceRegisterReturnsResultOfUnregisterPost() throws Exception {
        Token token = new Token("accessToken","refreshToken");
        String deviceId = "device-id";

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CONFIGURATION.path())
                        .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")
                        .withContent(Collections.singletonMap("conflicting_device", Collections.singletonMap("device_id", deviceId)))),
                eq(Configuration.class))).thenReturn(configuration);

        assertThat(operations.forceRegisterDevice(token, deviceId)).isSameAs(configuration.deviceManagement);
    }

    @Test
    public void saveConfigurationStoresFeaturesPlanAndExperiments() {
        final Configuration authorized = getAuthorizedConfiguration();

        operations.saveConfiguration(authorized);

        verify(featureOperations).updateFeatures(TestFeatures.asList());
        verify(featureOperations).updatePlan(authorized.plan.id, authorized.plan.upsells);
        verify(experimentOperations).update(authorized.assignment);
    }

    private Configuration getNoPlanConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), new UserPlan("none", null),
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, null));
    }

    private Configuration getAuthorizedConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), new UserPlan("mid_tier", null),
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, null));
    }

}