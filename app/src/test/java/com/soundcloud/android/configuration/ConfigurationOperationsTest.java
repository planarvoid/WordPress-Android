package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationOperations.CONFIGURATION_STALE_TIME_MILLIS;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.geq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
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
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class ConfigurationOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRx apiClientRx;
    @Mock private ApiClient apiClient;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private DeviceManagementStorage deviceManagementStorage;
    @Mock private ConfigurationSettingsStorage configurationSettingsStorage;

    private ConfigurationOperations operations;
    private Configuration configuration;

    private TestSubscriber<Configuration> configSubscriber = new TestSubscriber<>();
    private TestSubscriber<Object> subscriber = new TestSubscriber<>();
    private TestScheduler scheduler = new TestScheduler();

    @Before
    public void setUp() throws Exception {
        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(InjectionSupport.lazyOf(apiClientRx), InjectionSupport.lazyOf(apiClient),
                experimentOperations, featureOperations, featureFlags, configurationSettingsStorage, scheduler);

        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(true);
    }

    @Test
    public void updateReturnsConfiguration() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.update().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        configSubscriber.assertValue(noPlan);
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateSavesUpdateTimestamp() {
        final Configuration noPlan = getNoPlanConfiguration();
        final long now = System.currentTimeMillis();
        when(configurationSettingsStorage.getLastConfigurationCheckTime())
                .thenReturn(now - CONFIGURATION_STALE_TIME_MILLIS - 1);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan));

        operations.update().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(configurationSettingsStorage).setLastConfigurationCheckTime(geq(now));
    }

    @Test
    public void updateIfNecessaryUpdatesConfigurationIfLastUpdateTooLongAgo() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(configurationSettingsStorage.getLastConfigurationCheckTime())
                .thenReturn(System.currentTimeMillis() - CONFIGURATION_STALE_TIME_MILLIS - 1);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan));

        operations.updateIfNecessary().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        configSubscriber.assertValue(noPlan);
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateIfNecessaryDoesNotUpdateConfigurationIfRecentlyUpdated() {
        when(configurationSettingsStorage.getLastConfigurationCheckTime())
                .thenReturn(System.currentTimeMillis() - CONFIGURATION_STALE_TIME_MILLIS + 1);

        operations.updateIfNecessary().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        configSubscriber.assertNoValues();
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateIfNecessaryDoesNotSaveUpdateTimestampIfRecentlyUpdated() {
        when(configurationSettingsStorage.getLastConfigurationCheckTime())
                .thenReturn(System.currentTimeMillis() - CONFIGURATION_STALE_TIME_MILLIS + 1);

        operations.updateIfNecessary().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(configurationSettingsStorage, never()).setLastConfigurationCheckTime(anyLong());
    }

    @Test
    public void updateUntilPlanChangedReturnsExpectedConfigurationOnFirstAttempt() {
        final Configuration highTier = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(highTier));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(configSubscriber.getOnNextEvents().get(0)).isSameAs(highTier);
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateUntilPlanChangedStopsPollingWhenExpectedPlanIsReturned() {
        final Configuration noPlan = getNoPlanConfiguration();
        final Configuration withPlan = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan), Observable.just(withPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        configSubscriber.assertNoValues();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        configSubscriber.assertValueCount(1);
        assertThat(configSubscriber.getOnNextEvents().get(0)).isSameAs(withPlan);
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateUntilPlanChangedSavedConfigurationWhenExpectedPlanIsReturned() {
        final Configuration withPlan = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(withPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(featureOperations).updatePlan(eq(Plan.HIGH_TIER), anyList());
    }

    @Test
    public void updateUntilPlanChangedFailsAfterThreeAttempts() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        configSubscriber.assertError(NoSuchElementException.class);
    }

    @Test
    public void updateUntilPlanChangedDoesNotSaveConfigurationIfFailed() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(featureOperations, never()).updatePlan(anyString(), anyList());
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update().subscribe(configSubscriber);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")), eq(Configuration.class));
    }

    @Test
    public void registerDeviceGetsConfigurationAndReturnsDeviceManagement() throws ApiRequestException, IOException, ApiMapperException {
        Token token = new Token("accessToken", "refreshToken");
        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                        .withQueryParam("experiment_layers", "android_listening", "ios")
                        .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")),
                eq(Configuration.class))).thenReturn(configuration);

        assertThat(operations.registerDevice(token)).isSameAs(configuration.deviceManagement);
    }

    @Test
    public void registerDeviceStoresConfiguration() throws Exception {
        Token token = new Token("accessToken", "refreshToken");
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
        Token token = new Token("accessToken", "refreshToken");

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CONFIGURATION.path())
                        .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")),
                eq(Configuration.class))).thenReturn(configuration);

        assertThat(operations.forceRegisterDevice(token)).isSameAs(configuration.deviceManagement);
    }

    @Test
    public void saveConfigurationStoresFeaturesPlanAndExperiments() {
        final Configuration authorized = getHighTierConfiguration();

        operations.saveConfiguration(authorized);

        verify(featureOperations).updateFeatures(TestFeatures.asList());
        verify(featureOperations).updatePlan(authorized.plan.id, authorized.plan.upsells);
        verify(experimentOperations).update(authorized.assignment);
    }

    @Test
    public void deregisterDeviceCallsDeleteOnConfiguration() {
        when(apiClientRx.response(argThat(isApiRequestTo("DELETE", ApiEndpoints.CONFIGURATION.path()))))
                .thenReturn(Observable.just(TestApiResponses.ok()));

        operations.deregisterDevice().subscribe(subscriber);

        subscriber.assertValueCount(1);
    }

    @Test
    public void deregisterDeviceIgnoresFailure() {
        when(apiClientRx.response(argThat(isApiRequestTo("DELETE", ApiEndpoints.CONFIGURATION.path()))))
                .thenReturn(Observable.just(TestApiResponses.networkError()));

        operations.deregisterDevice().subscribe(subscriber);

        subscriber.assertNoErrors();
    }

    private Configuration getNoPlanConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), new UserPlan(Plan.NONE, null),
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, false));
    }

    private Configuration getHighTierConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), new UserPlan(Plan.HIGH_TIER, null),
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, false));
    }

}
