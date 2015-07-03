package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
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
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationOperationsTest {

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

    @Before
    public void setUp() throws Exception {
        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(InjectionSupport.lazyOf(apiClientRx), InjectionSupport.lazyOf(apiClient),
                experimentOperations, featureOperations, accountOperations, offlineContentOperations,
                deviceManagementStorage, featureFlags, Schedulers.immediate());

        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(true);
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update();

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

        expect(operations.registerDevice(token)).toBe(configuration.deviceManagement);
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

        expect(operations.forceRegisterDevice(token, deviceId)).toBe(configuration.deviceManagement);
    }

    @Test
    public void updateStoresConfiguration() {
        final Configuration authorized = getAuthorizedConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class))).thenReturn(Observable.just(authorized));
        operations.update();

        verify(featureOperations).updateFeatures(TestFeatures.asList());
        verify(featureOperations).updatePlan(authorized.plan.id, authorized.plan.upsells);
        verify(experimentOperations).update(authorized.assignment);
    }

    private Configuration getAuthorizedConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), new UserPlan("mid_tier", null),
                ConfigurationBlueprint.createLayers(), new DeviceManagement(true, null));
    }

    @Test
    public void updateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() throws Exception {
        Configuration configurationWithDeviceConflict = new Configuration(Collections.<Feature>emptyList(),
                new UserPlan("free", Arrays.asList("mid_tier")), Collections.<Layer>emptyList(), new DeviceManagement(false, null));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(Configuration.class)))
                .thenReturn(Observable.just(configurationWithDeviceConflict));

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);

        operations.update();

        logoutSubject.onNext(null);
        expect(clearOfflineContentSubject.hasObservers()).toBeTrue();
    }

}