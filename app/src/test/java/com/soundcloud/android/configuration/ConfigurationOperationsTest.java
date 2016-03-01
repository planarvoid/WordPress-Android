package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationOperations.CONFIGURATION_STALE_TIME_MILLIS;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.geq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
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
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import com.soundcloud.android.utils.Sleeper;
import com.soundcloud.android.utils.TryWithBackOff;
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
    @Mock private PlanChangeDetector planChangeDetector;
    @Mock private FeatureFlags featureFlags;
    @Mock private DeviceManagementStorage deviceManagementStorage;
    @Mock private ConfigurationSettingsStorage configurationSettingsStorage;
    @Mock private Sleeper sleeper;

    private ConfigurationOperations operations;
    private Configuration configuration;

    private TestSubscriber<Configuration> configSubscriber = new TestSubscriber<>();
    private TestSubscriber<Object> subscriber = new TestSubscriber<>();
    private TestScheduler scheduler = new TestScheduler();

    @Before
    public void setUp() throws Exception {
        when(apiClientRx.getApiClient()).thenReturn(apiClient);
        configuration = ModelFixtures.create(Configuration.class);
        TryWithBackOff.Factory factory = new TryWithBackOff.Factory(sleeper);
        operations = new ConfigurationOperations(apiClientRx,
                experimentOperations, featureOperations, planChangeDetector, featureFlags, configurationSettingsStorage,
                factory.<Configuration>create(0, TimeUnit.SECONDS, 0, 1), scheduler);

        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(configuration);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(configuration));
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureFlags.isEnabled(Flag.SOUNDCLOUD_GO)).thenReturn(true);
    }

    @Test
    public void updateReturnsConfiguration() throws Exception {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(noPlan);

        operations.update().subscribe(configSubscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        configSubscriber.assertValue(noPlan);
        configSubscriber.assertCompleted();
    }

    @Test
    public void updateIfNecessaryUpdatesConfigurationIfLastUpdateTooLongAgo() throws Exception {
        final Configuration noPlan = getNoPlanConfiguration();
        when(configurationSettingsStorage.getLastConfigurationCheckTime())
                .thenReturn(System.currentTimeMillis() - CONFIGURATION_STALE_TIME_MILLIS - 1);
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(noPlan);

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

        verify(configurationSettingsStorage, never()).setLastConfigurationUpdateTime(anyLong());
    }

    @Test
    public void awaitConfigurationWithPlanReturnsExpectedConfigurationOnFirstAttempt() {
        final Configuration highTier = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(highTier));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(configSubscriber.getOnNextEvents().get(0)).isSameAs(highTier);
        configSubscriber.assertCompleted();
    }

    @Test
    public void awaitConfigurationWithPlanStopsPollingWhenExpectedPlanIsReturned() {
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
    public void awaitConfigurationWithPlanSavesConfigurationWhenExpectedPlanIsReturned() {
        final Configuration withPlan = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(withPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(featureOperations).updatePlan(withPlan.userPlan);
    }

    @Test
    public void awaitConfigurationWithPlanFailsAfterThreeAttempts() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        configSubscriber.assertNoValues();
    }

    @Test
    public void awaitConfigurationWithPlanDoesNotSaveConfigurationIfFailed() {
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(featureOperations, never()).updatePlan(any(UserPlan.class));
    }

    @Test
    public void awaitConfigurationWithPlanReturnsEmptyWhenNoPlanChangeToHighTierAfterRetries() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.<Configuration>empty());

        operations.awaitConfigurationWithPlan(Plan.HIGH_TIER).subscribe(configSubscriber);
        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        configSubscriber.assertNoErrors();
        configSubscriber.assertNoValues();
        configSubscriber.assertCompleted();
    }

    @Test
    public void awaitConfigurationFromPendingPlanChangeAwaitsPendingHighTierUpgrade() {
        when(configurationSettingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.HIGH_TIER);
        final Configuration highTier = getHighTierConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(highTier));

        operations.awaitConfigurationFromPendingPlanChange().subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(configSubscriber.getOnNextEvents().get(0)).isSameAs(highTier);
        configSubscriber.assertCompleted();
    }

    @Test
    public void awaitConfigurationFromPendingPlanChangeAwaitsPendingDowngrade() {
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.FREE_TIER);
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationFromPendingPlanChange().subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(configSubscriber.getOnNextEvents().get(0)).isSameAs(noPlan);
        configSubscriber.assertCompleted();
    }

    @Test
    public void awaitConfigurationFromPendingPlanChangeFailsIfNoPendingChange() {
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.UNDEFINED);
        final Configuration noPlan = getNoPlanConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(noPlan));

        operations.awaitConfigurationFromPendingPlanChange().subscribe(configSubscriber);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        configSubscriber.assertNoValues();
        configSubscriber.assertError(IllegalStateException.class);
    }

    @Test
    public void loadsExperimentsOnUpdate() throws Exception {
        operations.update().subscribe(configSubscriber);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(apiClient).fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
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
        verify(featureOperations).updatePlan(configuration.userPlan);
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
        verify(featureOperations).updatePlan(authorized.userPlan);
        verify(experimentOperations).update(authorized.assignment);
    }

    @Test
    public void savingConfigurationStoresLastUpdateTimestamp() {
        final long now = System.currentTimeMillis();
        final Configuration authorized = getHighTierConfiguration();

        operations.saveConfiguration(authorized);

        verify(configurationSettingsStorage).setLastConfigurationUpdateTime(geq(now));
    }

    @Test
    public void hasPendingHighTierPlanUpgrade() {
        when(configurationSettingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.HIGH_TIER);
        assertThat(operations.isPendingHighTierUpgrade()).isTrue();
        when(configurationSettingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.UNDEFINED);
        assertThat(operations.isPendingHighTierUpgrade()).isFalse();
    }

    @Test
    public void hasPendingPlanDowngrade() {
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.FREE_TIER);
        assertThat(operations.isPendingDowngrade()).isTrue();
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.UNDEFINED);
        assertThat(operations.isPendingDowngrade()).isFalse();
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
        final UserPlan userPlan = new UserPlan(Plan.FREE_TIER.planId, Collections.<String>emptyList());
        return new Configuration(ConfigurationBlueprint.createFeatures(), userPlan,
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, false));
    }

    private Configuration getHighTierConfiguration() {
        final UserPlan userPlan = new UserPlan(Plan.HIGH_TIER.planId, Collections.<String>emptyList());
        return new Configuration(ConfigurationBlueprint.createFeatures(), userPlan,
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, false));
    }

}
